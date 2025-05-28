package com.jarindimick.handwashtracking.gui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build; // Needed for Build version check
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.jarindimick.handwashtracking.R;
import com.jarindimick.handwashtracking.databasehelper.DatabaseHelper;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public class ConfirmHandwashActivity extends AppCompatActivity {

    private static final String TAG = "ConfirmHandwashActivity";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int PERMISSION_REQUEST_CODE = 2;
    private static final long CAMERA_LAUNCH_DELAY_MS = 3000;
    private static final long CONFIRMATION_DISPLAY_DELAY_MS = 4000;

    private String employeeNumber;
    private String currentPhotoPath = "";

    private DatabaseHelper dbHelper;

    private TextView textStepTitleInitial;
    private TextView textStepInstructionsInitial;
    private LinearLayout layoutInitialInstructions;

    private LinearLayout layoutConfirmationMessage;
    private TextView textConfirmationMainMessage;
    private TextView textDailyCountMessage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_handwash);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        employeeNumber = getIntent().getStringExtra("employee_number");
        dbHelper = new DatabaseHelper(this);

        layoutInitialInstructions = findViewById(R.id.layout_initial_instructions);
        textStepTitleInitial = findViewById(R.id.text_step_title_initial);
        textStepInstructionsInitial = findViewById(R.id.text_step_instructions_initial);
        layoutConfirmationMessage = findViewById(R.id.layout_confirmation_message);
        textConfirmationMainMessage = findViewById(R.id.text_confirmation_main_message);
        textDailyCountMessage = findViewById(R.id.text_daily_count_message);

        layoutInitialInstructions.setVisibility(View.VISIBLE);
        layoutConfirmationMessage.setVisibility(View.GONE);

        Log.d(TAG, "onCreate: Activity created. Launching camera after delay.");
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Camera launch delay finished. Checking permissions.");
                checkCameraPermissionAndDispatchTakePictureIntent();
            }
        }, CAMERA_LAUNCH_DELAY_MS);
    }

    private void checkCameraPermissionAndDispatchTakePictureIntent() {
        Log.d(TAG, "Checking camera permission...");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission NOT granted. Requesting...");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
        } else {
            Log.d(TAG, "Camera permission already granted. Dispatching picture intent.");
            dispatchTakePictureIntent();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult called. RequestCode: " + requestCode);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean cameraPermissionGranted = false;
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Camera permission GRANTED by user.");
                cameraPermissionGranted = true;
            } else {
                Log.d(TAG, "Camera permission DENIED by user.");
            }

            if (cameraPermissionGranted) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Camera permission denied. Logging without photo.", Toast.LENGTH_SHORT).show();
                processHandwashResult("");
            }
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Log.d(TAG, "dispatchTakePictureIntent: Intent created for ACTION_IMAGE_CAPTURE.");

        // Attempt to use the front camera
        // For older APIs
        takePictureIntent.putExtra("android.intent.extras.CAMERA_FACING", android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT);
        // For newer APIs (though the above often still works or is preferred by some camera apps)
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // Camera.CameraInfo is deprecated but widely supported
        takePictureIntent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1);
        takePictureIntent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true);
        // }


        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            Log.d(TAG, "dispatchTakePictureIntent: Camera app IS available (resolveActivity is NOT null).");
            File photoFile = null;
            try {
                photoFile = createImageFile();
                Log.d(TAG, "Photo file created at: " + currentPhotoPath);
            } catch (IOException ex) {
                Log.e(TAG, "Error creating image file", ex);
                Toast.makeText(this, "Error creating photo file. Logging without photo.", Toast.LENGTH_LONG).show();
                processHandwashResult("");
                return;
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.jarindimick.handwashtracking.fileprovider",
                        photoFile);
                Log.d(TAG, "Photo URI: " + photoURI.toString());
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                Log.d(TAG, "startActivityForResult for camera called.");
            }
        } else {
            Log.e(TAG, "dispatchTakePictureIntent: Camera app NOT available (resolveActivity is null).");
            Toast.makeText(this, "No camera app found! Logging without photo.", Toast.LENGTH_SHORT).show();
            processHandwashResult("");
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = new File(getFilesDir(), "pictures");
        if (!storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                Log.e(TAG, "Failed to create directory for pictures: " + storageDir.getAbsolutePath());
                throw new IOException("Failed to create directory: " + storageDir.getAbsolutePath());
            }
        }
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: RequestCode: " + requestCode + ", ResultCode: " + resultCode);
        String finalPhotoPath = "";

        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK) {
                finalPhotoPath = currentPhotoPath;
                Log.d(TAG, "Photo captured successfully: " + finalPhotoPath);
            } else if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "Photo capture cancelled by user.");
                if (!currentPhotoPath.isEmpty()) {
                    File emptyFile = new File(currentPhotoPath);
                    if (emptyFile.exists() && emptyFile.length() == 0) {
                        Log.d(TAG, "Deleting empty/cancelled photo file: " + currentPhotoPath);
                        emptyFile.delete();
                    }
                }
                currentPhotoPath = "";
            } else {
                Log.e(TAG, "Photo capture failed with resultCode: " + resultCode);
                if (!currentPhotoPath.isEmpty()) {
                    File photoFile = new File(currentPhotoPath);
                    if (photoFile.exists()) {
                        Log.d(TAG, "Deleting failed photo file: " + currentPhotoPath);
                        photoFile.delete();
                    }
                }
                currentPhotoPath = "";
            }
            processHandwashResult(finalPhotoPath);
        }
    }

    private void processHandwashResult(String photoPath) {
        Log.d(TAG, "Processing handwash result. Photo path: " + (photoPath == null || photoPath.isEmpty() ? "N/A" : photoPath));
        saveHandwashLogWithPhoto(photoPath);
        int dailyCount = dbHelper.getHandwashCountForEmployeeToday(employeeNumber);

        layoutInitialInstructions.setVisibility(View.GONE);
        layoutConfirmationMessage.setVisibility(View.VISIBLE);

        if (photoPath != null && !photoPath.isEmpty()) {
            textConfirmationMainMessage.setText("Handwash Logged!");
        } else {
            textConfirmationMainMessage.setText("Handwash Logged (No Photo)");
        }
        textDailyCountMessage.setText(String.format(Locale.getDefault(), "Your daily count: %d", dailyCount));
        Log.d(TAG, "Displaying confirmation message. Daily count: " + dailyCount);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Confirmation display delay finished. Navigating to main screen.");
                navigateToMainScreen();
            }
        }, CONFIRMATION_DISPLAY_DELAY_MS);
    }


    private void saveHandwashLogWithPhoto(String photoPath) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault());
        String washDate = now.format(dateFormatter);
        String washTime = now.format(timeFormatter);

        long logResult = dbHelper.insertHandwashLog(employeeNumber, washDate, washTime, photoPath == null ? "" : photoPath);

        if (logResult == -1) {
            Log.e(TAG, "Error saving handwash log to database. Employee: " + employeeNumber + ", Path: " + photoPath);
        } else {
            Log.d(TAG, "Handwash log saved successfully. ID: " + logResult);
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
        if (dbHelper != null) {
            dbHelper.close();
        }
        Log.d(TAG, "onDestroy called.");
    }
}
