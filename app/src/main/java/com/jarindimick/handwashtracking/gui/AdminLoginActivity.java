package com.jarindimick.handwashtracking.gui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.jarindimick.handwashtracking.R;
import com.jarindimick.handwashtracking.databasehelper.DatabaseHelper; // Import DatabaseHelper


public class AdminLoginActivity extends AppCompatActivity {

    private EditText edit_username;
    private EditText edit_password;
    private Button btn_login;
    private Button btn_return_to_main;
    private DatabaseHelper dbHelper; // Add DatabaseHelper instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        setupgui();
        dbHelper = new DatabaseHelper(this); // Initialize DatabaseHelper
        setupListeners();
    }

    private void setupgui() {
        edit_username = findViewById(R.id.edit_username);
        edit_password = findViewById(R.id.edit_password);
        btn_login = findViewById(R.id.btn_login);
        btn_return_to_main = findViewById(R.id.btn_return_to_main);

        //Hide action bar
        getSupportActionBar().hide();
    }

    private void setupListeners() {
        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = edit_username.getText().toString();
                String password = edit_password.getText().toString();

                // *** REPLACE THIS WITH API CALL LATER ***
                //if (username.equals("admin") && password.equals("admin")) {  // Old insecure check
                if (dbHelper.validateAdminLogin(username, password)) {  // Use the new validation method
                    // Successful login
                    Toast.makeText(AdminLoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                    //  Navigate to Admin Dashboard Activity (IMPLEMENT LATER)
                    Intent intent = new Intent(AdminLoginActivity.this, AdminDashboardActivity.class);
                    startActivity(intent);
                    finish(); // Close the login activity so the user can't go back with the back button
                } else {
                    // Invalid credentials
                    Toast.makeText(AdminLoginActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btn_return_to_main.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Return to the main handwashing activity
                finish(); // Simply finish the current activity to go back
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close(); // Close the database connection
        }
    }
}