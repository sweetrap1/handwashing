package com.jarindimick.handwashtracking.gui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast; // Keep for successful login Toast

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); //
        setContentView(R.layout.activity_admin_login); //

        View mainView = findViewById(R.id.main);
        // Capture initial padding values from the XML layout
        final int initialPaddingLeft = mainView.getPaddingLeft();
        final int initialPaddingTop = mainView.getPaddingTop();
        final int initialPaddingRight = mainView.getPaddingRight();
        final int initialPaddingBottom = mainView.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Apply system bar insets ADDED TO the initial XML padding
            v.setPadding(initialPaddingLeft + systemBars.left,
                    initialPaddingTop + systemBars.top,
                    initialPaddingRight + systemBars.right,
                    initialPaddingBottom + systemBars.bottom);
            return WindowInsetsCompat.CONSUMED; // Consume the insets as they've been applied
        });

        setupgui(); //
        dbHelper = new DatabaseHelper(this); //
        setupListeners(); //
    }

    private void setupgui() {
        edit_username = findViewById(R.id.edit_username); //
        edit_password = findViewById(R.id.edit_password); //
        btn_login = findViewById(R.id.btn_login); //
        btn_return_to_main = findViewById(R.id.btn_return_to_main); //
    }

    private void setupListeners() {
        btn_login.setOnClickListener(new View.OnClickListener() { //
            @Override
            public void onClick(View v) {
                String username = edit_username.getText().toString().trim();
                String password = edit_password.getText().toString().trim();

                // Clear previous errors
                edit_username.setError(null);
                edit_password.setError(null);

                boolean isValid = true;
                if (username.isEmpty()) {
                    edit_username.setError("Please enter username");
                    isValid = false;
                }
                if (password.isEmpty()) {
                    edit_password.setError("Please enter password");
                    isValid = false;
                }

                if (!isValid) {
                    if (username.isEmpty()) edit_username.requestFocus();
                    else if (password.isEmpty()) edit_password.requestFocus();
                    return;
                }

                if (dbHelper.validateAdminLogin(username, password)) { //
                    Toast.makeText(AdminLoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show(); //
                    Intent intent = new Intent(AdminLoginActivity.this, AdminDashboardActivity.class); //
                    startActivity(intent); //
                    finish(); //
                } else {
                    // Toast.makeText(AdminLoginActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show(); // OLD
                    edit_password.setError("Invalid username or password"); // NEW
                    edit_password.requestFocus();
                }
            }
        });

        btn_return_to_main.setOnClickListener(new View.OnClickListener() { //
            @Override
            public void onClick(View v) {
                finish(); //
            }
        });

        edit_password.setOnEditorActionListener(new TextView.OnEditorActionListener() { //
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                        (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    if (btn_login != null) {
                        btn_login.performClick();
                    }
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE); //
                    if (imm != null && getCurrentFocus() != null) {
                        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                    }
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close(); //
        }
    }
}