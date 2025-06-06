package com.jarindimick.handwashtracking.gui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.jarindimick.handwashtracking.R;
import com.jarindimick.handwashtracking.databasehelper.DatabaseHelper;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConfirmHandwashActivity extends AppCompatActivity {

    private static final String TAG = "ConfirmHandwashActivity";
    private static final int PERMISSION_REQUEST_CODE_CAMERA = 101;
    private static final long MOTION_DETECTION_DURATION_MS = 4000; // Detect for 4 seconds
    private static final long CONFIRMATION_DISPLAY_MS = 2000;

    private String employeeNumber;
    private DatabaseHelper dbHelper;

    private LinearLayout layoutInitialInstructions;
    private LinearLayout layoutConfirmationMessage;
    private TextView textConfirmationMainMessage;
    private TextView textDailyCountMessage;

    // ADD THE isFinalized LINE
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean motionDetected = new AtomicBoolean(false);
    private final AtomicBoolean isFinalized = new AtomicBoolean(false); // <-- ADD THIS LINE
    private ProcessCameraProvider cameraProvider;
    private ImageAnalysis imageAnalysis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_handwash);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        employeeNumber = getIntent().getStringExtra("employee_number");
        dbHelper = new DatabaseHelper(this);

        layoutInitialInstructions = findViewById(R.id.layout_initial_instructions);
        layoutConfirmationMessage = findViewById(R.id.layout_confirmation_message);
        textConfirmationMainMessage = findViewById(R.id.text_confirmation_main_message);
        textDailyCountMessage = findViewById(R.id.text_daily_count_message);

        if (checkCameraPermission()) {
            setupCamera();
        } else {
            requestCameraPermission();
        }
    }

    private void setupCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                startMotionDetection();
            } catch (Exception e) {
                Log.e(TAG, "Could not setup camera", e);
                processHandwashCompletion("Camera Setup Error");
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void startMotionDetection() {
        if (cameraProvider == null) {
            processHandwashCompletion("Camera Provider Error");
            return;
        }
        cameraProvider.unbindAll();

        imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, new MotionDetectionAnalyzer());

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();

        try {
            cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageAnalysis);
            Log.d(TAG, "ImageAnalysis use case bound.");

            mainHandler.postDelayed(() -> {
                if (isFinishing()) return;
                imageAnalysis.clearAnalyzer();
                cameraProvider.unbindAll();
                String result = motionDetected.get() ? "Motion Confirmed" : "No Motion Detected";
                processHandwashCompletion(result);
            }, MOTION_DETECTION_DURATION_MS);

        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
            processHandwashCompletion("Camera Binding Error");
        }
    }

    private class MotionDetectionAnalyzer implements ImageAnalysis.Analyzer {
        private byte[] previousFrame = null;

        @Override
        public void analyze(@NonNull ImageProxy image) {
            if (motionDetected.get()) {
                image.close();
                return;
            }

            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);

            if (previousFrame != null) {
                long motionPixelCount = 0;
                for (int i = 0; i < data.length; i += 20) {
                    if (Math.abs(data[i] - previousFrame[i]) > 65) {
                        motionPixelCount++;
                    }
                }
                double motionPercentage = (double) motionPixelCount / (data.length / 20.0);
                if (motionPercentage > 0.05) {
                    Log.d(TAG, "Motion detected!");
                    motionDetected.set(true);
                }
            }
            previousFrame = data;
            image.close();
        }
    }

    private void processHandwashCompletion(String confirmationStatus) {
        if (isFinalized.getAndSet(true)) {
            return;
        }
        mainHandler.removeCallbacksAndMessages(null);

        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }

        layoutInitialInstructions.setVisibility(View.GONE);
        layoutConfirmationMessage.setVisibility(View.VISIBLE);
        textConfirmationMainMessage.setText("Handwash Logged!");

        // This method now handles updating the text on the screen
        saveHandwashLogToDb(confirmationStatus);

        mainHandler.postDelayed(this::navigateToMainScreen, CONFIRMATION_DISPLAY_MS);
    }

    private void saveHandwashLogToDb(String confirmationStatus) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault());
        String washDate = now.format(dateFormatter);
        String washTime = now.format(timeFormatter);

        // First, save the log to the database
        long logResult = dbHelper.insertHandwashLog(employeeNumber, washDate, washTime, confirmationStatus);
        if (logResult == -1) {
            Log.e(TAG, "Error saving log.");
        } else {
            Log.d(TAG, "Log saved successfully.");
        }

        // --- NEW HOURLY COMPLIANCE FEEDBACK ---
        // Now that the log is saved, check the counts
        int dailyCount = dbHelper.getHandwashCountForEmployeeToday(employeeNumber);
        boolean alreadyWashedThisHour = dbHelper.hasWashedInCurrentHour(employeeNumber, washDate, washTime);

        // Find the TextView for the daily count message
        TextView textDailyCountMessage = findViewById(R.id.text_daily_count_message);

        // Create a more informative message
        String message = String.format(Locale.getDefault(), "Your daily count: %d", dailyCount);
        if (!alreadyWashedThisHour) {
            // Add a positive reinforcement message if this is the first wash in the current hour
            message += "\n\nGreat job on your first wash this hour!";
        }
        textDailyCountMessage.setText(message);
        // --- END OF NEW LOGIC ---
    }

    private void navigateToMainScreen() {
        if (isFinishing()) return;
        Intent intent = new Intent(ConfirmHandwashActivity.this, MainHandwashing.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE_CAMERA);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupCamera();
            } else {
                Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_LONG).show();
                processHandwashCompletion("Camera Permission Denied");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }
}