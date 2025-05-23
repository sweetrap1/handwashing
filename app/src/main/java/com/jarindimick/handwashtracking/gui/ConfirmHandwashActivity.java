package com.jarindimick.handwashtracking.gui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.widget.Toast;

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

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int PERMISSION_REQUEST_CODE = 2;
    private static final long CAMERA_LAUNCH_DELAY_MS = 1000; // 1 second delay before launching camera

    private String employeeNumber;
    private String currentPhotoPath = ""; // Initialize to empty string

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_handwash);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        employeeNumber = getIntent().getStringExtra("employee_number");
        dbHelper = new DatabaseHelper(this);

        // Automatically launch camera after a delay
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkCameraPermissionAndDispatchTakePictureIntent();
            }
        }, CAMERA_LAUNCH_DELAY_MS);
    }

    private void checkCameraPermissionAndDispatchTakePictureIntent() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
        } else {
            dispatchTakePictureIntent();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Camera permission denied. Handwash will be logged without a photo.", Toast.LENGTH_LONG).show();
                // Log handwash without photo if permission denied
                saveHandwashLogWithPhoto("");
                navigateToMainScreen();
            }
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Toast.makeText(this, "Error creating photo file. Handwash will be logged without a photo.", Toast.LENGTH_LONG).show();
                // Log handwash without photo in case of file creation error
                saveHandwashLogWithPhoto("");
                navigateToMainScreen();
                return;
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.jarindimick.handwashtracking.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        } else {
            Toast.makeText(this, "No camera app found! Handwash will be logged without a photo.", Toast.LENGTH_SHORT).show();
            // Log handwash without photo if no camera app found
            saveHandwashLogWithPhoto("");
            navigateToMainScreen();
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        // Using getFilesDir() for internal storage to avoid external storage permissions on newer APIs
        File storageDir = new File(getFilesDir(), "pictures"); // Matches the path in file_paths.xml
        if (!storageDir.exists()) {
            storageDir.mkdirs(); // Create the directory if it doesn't exist
        }
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String finalPhotoPath = "";
        String toastMessage = "";

        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK) {
                finalPhotoPath = currentPhotoPath;
                toastMessage = "Handwash confirmed and picture saved!";
            } else if (resultCode == RESULT_CANCELED) {
                toastMessage = "Photo capture cancelled. Handwash logged without a photo.";
            } else {
                toastMessage = "Failed to capture photo. Handwash logged without a photo.";
            }

            // Always save handwash log regardless of photo result
            saveHandwashLogWithPhoto(finalPhotoPath);

            // Get and display daily handwash count
            int dailyCount = dbHelper.getHandwashCountForEmployeeToday(employeeNumber);
            Toast.makeText(this, toastMessage + "\nYou have washed your hands " + dailyCount + " time(s) today!", Toast.LENGTH_LONG).show();

            // Always navigate back to the main screen
            navigateToMainScreen();
        }
    }

    private void saveHandwashLogWithPhoto(String photoPath) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault());
        String washDate = now.format(dateFormatter);
        String washTime = now.format(timeFormatter);

        long logResult = dbHelper.insertHandwashLog(employeeNumber, washDate, washTime, photoPath);

        if (logResult == -1) {
            Toast.makeText(this, "Error confirming handwash or saving picture path to database.", Toast.LENGTH_LONG).show();
        }
    }

    private void navigateToMainScreen() {
        Intent intent = new Intent(ConfirmHandwashActivity.this, MainHandwashing.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Clears the activity stack
        startActivity(intent);
        finish(); // Finish this activity
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}