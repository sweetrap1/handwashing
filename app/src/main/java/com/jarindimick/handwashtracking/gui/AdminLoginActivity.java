package com.jarindimick.handwashtracking.gui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // Optional: if you add a Toolbar
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.jarindimick.handwashtracking.R;
import com.jarindimick.handwashtracking.databasehelper.DatabaseHelper;

public class AdminLoginActivity extends AppCompatActivity {

    private EditText edit_username;
    private EditText edit_password;
    private Button btn_login;
    private Button btn_return_to_main;
    private DatabaseHelper dbHelper;
    // Optional: private Toolbar adminLoginToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_login);

        // Apply window insets listener to the root content view (R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // The R.id.main is the ConstraintLayout which already has padding="16dp"
            // We add the system bar insets to this existing padding.
            // Or, if R.id.main had padding 0dp, it would be:
            // v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            v.setPadding(
                    systemBars.left + v.getPaddingLeft(),
                    systemBars.top + v.getPaddingTop(),
                    systemBars.right + v.getPaddingRight(),
                    systemBars.bottom + v.getPaddingBottom()
            );
            return insets;
        });

        // If you applied NoActionBar theme in Manifest and added a Toolbar to XML:
        // adminLoginToolbar = findViewById(R.id.admin_login_toolbar);
        // setSupportActionBar(adminLoginToolbar);
        // if (getSupportActionBar() != null) {
        // getSupportActionBar().setDisplayShowTitleEnabled(true); // Or false, or set title on Toolbar
        // getSupportActionBar().setTitle("Admin Login"); // Example
        // }

        // If NoActionBar theme is applied via Manifest, no default ActionBar to hide.
        // If you are using the default theme (with DarkActionBar), then getSupportActionBar().hide() is needed.
        // Assuming Theme.HandwashTracking.NoActionBar is applied via Manifest from previous step:
        // if (getSupportActionBar() != null) getSupportActionBar().hide(); // Not needed with NoActionBar theme

        setupgui();
        dbHelper = new DatabaseHelper(this);
        setupListeners();
    }

    private void setupgui() {
        edit_username = findViewById(R.id.edit_username);
        edit_password = findViewById(R.id.edit_password);
        btn_login = findViewById(R.id.btn_login);
        btn_return_to_main = findViewById(R.id.btn_return_to_main);

        // If NOT using NoActionBar theme from Manifest and want to hide default ActionBar:
        // if (getSupportActionBar() != null) {
        //     getSupportActionBar().hide();
        // }
    }

    private void setupListeners() {
        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = edit_username.getText().toString().trim();
                String password = edit_password.getText().toString().trim();

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(AdminLoginActivity.this, "Please enter username and password", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (dbHelper.validateAdminLogin(username, password)) {
                    Toast.makeText(AdminLoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(AdminLoginActivity.this, AdminDashboardActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(AdminLoginActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btn_return_to_main.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}
