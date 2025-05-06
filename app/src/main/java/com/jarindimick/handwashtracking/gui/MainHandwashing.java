package com.jarindimick.handwashtracking.gui;

import android.content.Intent; // Keep this import
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog; // Keep this import
import android.view.LayoutInflater; // Keep this import

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.jarindimick.handwashtracking.R;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import android.app.AlertDialog; // Add this
import android.view.LayoutInflater; // Add this
import android.content.Intent; // Make sure this is also imported for the Intent

public class MainHandwashing extends AppCompatActivity {
    private ImageView img_logo;
    private TextView txt_datetime;
    private EditText edit_employee_number;
    private Button btn_start;
    private Button btn_admin_login; // Declare btn_admin_login

    private Handler handler = new Handler();
    private Runnable updateTimeRunnable;
    private com.jarindimick.handwashtracking.gui.DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_handwashing);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        setupgui();
        startUpdatingTime();
        setupListeners();
        dbHelper = new com.jarindimick.handwashtracking.gui.DatabaseHelper(this); // Initialize DatabaseHelper
    }

    private void setupgui() {
        img_logo = findViewById(R.id.img_logo);
        txt_datetime = findViewById(R.id.txt_datetime);
        edit_employee_number = findViewById(R.id.edit_employee_number);
        btn_start = findViewById(R.id.btn_start);
        btn_admin_login = findViewById(R.id.btn_admin_login); // Initialize btn_admin_login
    }

    private void updateDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault());
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault());
        String formattedDate = now.format(dateFormatter);
        String formattedTime = now.format(timeFormatter);
        txt_datetime.setText(String.format("%s, %s", formattedDate, formattedTime));
    }

    private void startUpdatingTime() {
        updateTimeRunnable = new Runnable() {
            @Override
            public void run() {
                updateDateTime();
                handler.postDelayed(this, 1000); // Update every 1 second
            }
        };
        handler.postDelayed(updateTimeRunnable, 0); // Start immediately
    }

    private void setupListeners() {
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String employeeNumber = edit_employee_number.getText().toString();
                if (!employeeNumber.isEmpty()) {
                    saveEmployeeData(employeeNumber);
                } else {
                    Toast.makeText(MainHandwashing.this, "Please enter employee number", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Add the listener for the admin login button here
        btn_admin_login.setOnClickListener(new View.OnClickListener() { // <-- Added this listener
            @Override
            public void onClick(View v) {
                showAdminLoginDialog(); // <-- This calls the method defined below
            }
        });
    }

    private void saveEmployeeData(String employeeNumber) {
        long result = dbHelper.insertEmployee(employeeNumber);
        if (result != -1) {
            Toast.makeText(this, "Employee data saved", Toast.LENGTH_SHORT).show();
            edit_employee_number.setText(""); // Clear the input field
        } else {
            Toast.makeText(this, "Error saving employee data", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove the callback to prevent memory leaks
        handler.removeCallbacks(updateTimeRunnable);
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    // Add this method somewhere within your MainHandwashing class,
    // outside of other methods like onCreate, setupListeners, etc.
    // <-- The method definition starts here, OUTSIDE of setupListeners()
    private void showAdminLoginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_admin_login, null); // Make sure dialog_admin_login.xml exists

        // Set the custom layout for the dialog
        builder.setView(dialogView);

        // Get references to the EditText fields in the dialog layout
        EditText usernameEditText = dialogView.findViewById(R.id.edit_admin_username);
        EditText passwordEditText = dialogView.findViewById(R.id.edit_admin_password);

        // Add action buttons
        builder.setPositiveButton("Login", (dialog, id) -> {
            // This button is handled below to prevent auto-dismissal
        });
        builder.setNegativeButton("Cancel", (dialog, id) -> {
            // User cancelled the dialog, do nothing or handle accordingly
            dialog.cancel();
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        // Override the positive button's onClickListener to handle validation
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String enteredUsername = usernameEditText.getText().toString();
                String enteredPassword = passwordEditText.getText().toString();

                // --- Simple Hardcoded Credential Check (Replace with secure method in production) ---
                String correctUsername = "admin"; // Replace with your actual username
                String correctPassword = "admin"; // Replace with your actual password
                // ---------------------------------------------------------------------------------

                if (enteredUsername.equals(correctUsername) && enteredPassword.equals(correctPassword)) {
                    // Credentials match, navigate to Admin Dashboard
                    Toast.makeText(MainHandwashing.this, "Login Successful", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainHandwashing.this, AdminDashboardActivity.class);
                    startActivity(intent);
                    dialog.dismiss(); // Dismiss the dialog after successful login

                } else {
                    // Credentials do not match
                    Toast.makeText(MainHandwashing.this, "Invalid Username or Password", Toast.LENGTH_SHORT).show();
                    // Optionally, clear fields or shake inputs to indicate error
                }
            }
        });
    } // <-- The method definition ends here
}