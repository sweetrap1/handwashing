package com.jarindimick.handwashtracking.gui;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
// import androidx.camera.core.Preview; // REMOVED Preview import
import androidx.camera.lifecycle.ProcessCameraProvider;
// import androidx.camera.view.PreviewView; // REMOVED PreviewView import
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.jarindimick.handwashtracking.R;
import com.jarindimick.handwashtracking.databasehelper.DatabaseHelper;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConfirmHandwashActivity extends AppCompatActivity {

    private static final String TAG = "ConfirmHandwashActivity";
    private static final int PERMISSION_REQUEST_CODE_CAMERA = 101;
    private static final long INITIAL_SETUP_DELAY_MS = 1000; // Delay before starting camera setup (allows UI to settle)
    private static final long PHOTO_CAPTURE_DELAY_MS = 1000; // 3 seconds after camera is "ready" (use cases bound)
    private static final long CONFIRMATION_DISPLAY_MS = 1500;

    private String employeeNumber;
    private String currentPhotoPath = "";
    private Uri currentPhotoUri;

    private DatabaseHelper dbHelper;
    // private PreviewView cameraPreviewView; // REMOVED
    private LinearLayout layoutInitialInstructions;
    private LinearLayout layoutConfirmationMessage;
    private TextView textConfirmationMainMessage;
    private TextView textDailyCountMessage;
    private ProgressBar initialProgressSpinner;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;

    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_handwash);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        employeeNumber = getIntent().getStringExtra("employee_number");
        dbHelper = new DatabaseHelper(this);
        mainHandler = new Handler(Looper.getMainLooper());
        cameraExecutor = Executors.newSingleThreadExecutor();

        // cameraPreviewView = findViewById(R.id.camera_preview_view); // REMOVED
        layoutInitialInstructions = findViewById(R.id.layout_initial_instructions);
        layoutConfirmationMessage = findViewById(R.id.layout_confirmation_message);
        textConfirmationMainMessage = findViewById(R.id.text_confirmation_main_message);
        textDailyCountMessage = findViewById(R.id.text_daily_count_message);
        initialProgressSpinner = findViewById(R.id.initial_progress_spinner);

        layoutInitialInstructions.setVisibility(View.VISIBLE);
        initialProgressSpinner.setVisibility(View.VISIBLE);
        // cameraPreviewView.setVisibility(View.GONE); // REMOVED
        layoutConfirmationMessage.setVisibility(View.GONE);

        // Add a small delay before permission check/camera setup to ensure UI is stable
        mainHandler.postDelayed(() -> {
            if (checkCameraPermission()) {
                Log.d(TAG, "Camera permission already granted. Setting up camera.");
                setupCamera();
            } else {
                Log.d(TAG, "Camera permission NOT granted. Requesting...");
                requestCameraPermission();
            }
        }, INITIAL_SETUP_DELAY_MS);
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
                Log.d(TAG, "Camera permission GRANTED by user.");
                setupCamera();
            } else {
                Log.w(TAG, "Camera permission DENIED by user.");
                Toast.makeText(this, "Camera permission is required. Logging without photo.", Toast.LENGTH_LONG).show();
                processHandwashCompletion(null, "Camera Permission Denied");
            }
        }
    }

    private void setupCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error setting up camera provider: " + e.getMessage(), e);
                Toast.makeText(this, "Error initializing camera.", Toast.LENGTH_SHORT).show();
                processHandwashCompletion(null, "Camera Init Error");
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) {
            Log.e(TAG, "Camera provider not available.");
            Toast.makeText(this, "Camera not available.", Toast.LENGTH_SHORT).show();
            processHandwashCompletion(null, "Camera Provider Null");
            return;
        }

        // Preview use case is REMOVED
        // Preview preview = new Preview.Builder().build();
        // preview.setSurfaceProvider(cameraPreviewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        try {
            cameraProvider.unbindAll();
            // Bind only ImageCapture, no Preview
            cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageCapture);
            Log.d(TAG, "Camera ImageCapture use case bound (no preview).");

            // UI update: Hide spinner, update instructions text
            initialProgressSpinner.setVisibility(View.GONE);
            TextView textInstructions = findViewById(R.id.text_step_instructions_initial);
            if (textInstructions != null) {
                textInstructions.setText("Capturing photo... Please hold still."); // NEW TEXT
            }
            // layoutInitialInstructions remains visible with the updated instruction.
            // The title "Step 5: Confirm Handwash" also remains.

            // Schedule auto-capture after a delay
            mainHandler.postDelayed(this::takeAutoPicture, PHOTO_CAPTURE_DELAY_MS);

        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed (no preview): " + e.getMessage(), e);
            Toast.makeText(this, "Failed to start camera for capture.", Toast.LENGTH_SHORT).show();
            processHandwashCompletion(null, "Camera Binding Error");
        }
    }

    private void takeAutoPicture() {
        if (imageCapture == null) {
            Log.e(TAG, "ImageCapture use case is null. Cannot take picture.");
            processHandwashCompletion(null, "ImageCapture Null");
            return;
        }

        ImageCapture.OutputFileOptions outputFileOptions;
        String photoFileName = "HANDWASH_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, photoFileName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "HandwashApp");

            outputFileOptions = new ImageCapture.OutputFileOptions.Builder(
                    getContentResolver(),
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
            ).build();
        } else {
            File appSpecificDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (appSpecificDir == null) {
                Log.e(TAG, "App-specific external pictures directory not available for pre-Q.");
                Toast.makeText(this, "Storage not available (pre-Q).", Toast.LENGTH_SHORT).show();
                processHandwashCompletion(null, "Storage Error (pre-Q)");
                return;
            }
            if (!appSpecificDir.exists() && !appSpecificDir.mkdirs()) {
                Log.e(TAG, "Failed to create app-specific pictures directory for pre-Q: " + appSpecificDir.getAbsolutePath());
            }
            File photoFile = new File(appSpecificDir, photoFileName + ".jpg");
            currentPhotoPath = photoFile.getAbsolutePath();
            outputFileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();
        }

        Log.d(TAG, "Attempting to take picture automatically (no preview)...");
        imageCapture.takePicture(outputFileOptions, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                currentPhotoUri = outputFileResults.getSavedUri();
                if (currentPhotoUri == null && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    if (!currentPhotoPath.isEmpty()) {
                        File imageFile = new File(currentPhotoPath);
                        try {
                            currentPhotoUri = FileProvider.getUriForFile(ConfirmHandwashActivity.this,
                                    getApplicationContext().getPackageName() + ".fileprovider",
                                    imageFile);
                        } catch (IllegalArgumentException e) {
                            Log.e(TAG, "FileProvider error for pre-Q image: " + e.getMessage(), e);
                        }
                    }
                }

                String photoIdentifier = (currentPhotoUri != null) ? currentPhotoUri.toString() : currentPhotoPath;
                Log.d(TAG, "Photo capture successful: " + photoIdentifier);
                mainHandler.post(() -> processHandwashCompletion(photoIdentifier, null));
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);
                mainHandler.post(() -> {
                    Toast.makeText(ConfirmHandwashActivity.this, "Failed to capture photo: " + exception.getImageCaptureError(), Toast.LENGTH_LONG).show();
                    processHandwashCompletion(null, "Capture Error: " + exception.getImageCaptureError());
                });
            }
        });
    }

    private void processHandwashCompletion(String photoIdentifier, String errorMessage) {
        // cameraPreviewView.setVisibility(View.GONE); // REMOVED
        layoutInitialInstructions.setVisibility(View.GONE); // Hide initial instructions now

        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }

        String finalPhotoPathForDb = "";
        if (photoIdentifier != null && !photoIdentifier.isEmpty()) {
            finalPhotoPathForDb = photoIdentifier;
            textConfirmationMainMessage.setText("Handwash Logged!");
        } else {
            textConfirmationMainMessage.setText("Handwash Logged (No Photo)");
            if (errorMessage != null) {
                textConfirmationMainMessage.append("\nError: " + errorMessage);
            }
        }

        saveHandwashLogToDb(finalPhotoPathForDb);
        int dailyCount = dbHelper.getHandwashCountForEmployeeToday(employeeNumber);
        textDailyCountMessage.setText(String.format(Locale.getDefault(), "Your daily count: %d", dailyCount));

        layoutConfirmationMessage.setVisibility(View.VISIBLE);

        mainHandler.postDelayed(this::navigateToMainScreen, CONFIRMATION_DISPLAY_MS);
    }

    private void saveHandwashLogToDb(String photoPathForDb) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault());
        String washDate = now.format(dateFormatter);
        String washTime = now.format(timeFormatter);

        long logResult = dbHelper.insertHandwashLog(employeeNumber, washDate, washTime, photoPathForDb);
        if (logResult == -1) {
            Log.e(TAG, "Error saving handwash log to database. Emp: " + employeeNumber + ", Path: " + photoPathForDb);
        } else {
            Log.d(TAG, "Handwash log saved successfully to DB. ID: " + logResult);
        }
    }

    private void navigateToMainScreen() {
        Intent intent = new Intent(ConfirmHandwashActivity.this, MainHandwashing.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        Log.d(TAG, "onDestroy called.");
    }
}