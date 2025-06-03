package com.jarindimick.handwashtracking.gui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Spanned; // For styled text
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView; // For text views

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat; // For better HTML parsing
import androidx.core.graphics.Insets; // For window insets
import androidx.core.view.ViewCompat;    // For window insets
import androidx.core.view.WindowInsetsCompat; // For window insets

import com.jarindimick.handwashtracking.R;

public class WelcomeActivity extends AppCompatActivity {

    // Constants for SharedPreferences
    public static final String PREFS_WELCOME_FILE = "WelcomeScreenPrefs";
    public static final String KEY_WELCOME_SCREEN_SHOWN_ONCE = "welcomeScreenShownOnce";

    private CheckBox checkboxDontShowAgain;
    private Button btnContinueWelcome;

    // TextViews for dynamic content if needed, or just use strings.xml
    private TextView txtWelcomeGuestDesc, txtWelcomeAdminDescLogin, txtWelcomeAdminDescCreds,
            txtWelcomeAdminDescPass, txtWelcomeFreeEmployees, txtWelcomeFreeNoImport,
            txtWelcomeFreeNoLogo, txtWelcomeFreeConsiderPro;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        // Check if the welcome screen should be skipped
        SharedPreferences prefs = getSharedPreferences(PREFS_WELCOME_FILE, MODE_PRIVATE);
        if (prefs.getBoolean(KEY_WELCOME_SCREEN_SHOWN_ONCE, false)) {
            navigateToMainApp();
            return; // Important: Do not proceed to setContentView for this activity
        }

        setContentView(R.layout.activity_welcome);

        // Apply window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scroll_view_welcome).getRootView(), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left + v.getPaddingLeft(), insets.top, insets.right + v.getPaddingRight(), insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });


        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize views
        checkboxDontShowAgain = findViewById(R.id.checkbox_dont_show_welcome);
        btnContinueWelcome = findViewById(R.id.btn_continue_welcome);

        // Set text using HtmlCompat to render bold tags from strings.xml
        txtWelcomeGuestDesc = findViewById(R.id.txt_welcome_guest_desc);
        txtWelcomeAdminDescLogin = findViewById(R.id.txt_welcome_admin_desc_login);
        txtWelcomeAdminDescCreds = findViewById(R.id.txt_welcome_admin_desc_creds);
        txtWelcomeAdminDescPass = findViewById(R.id.txt_welcome_admin_desc_pass);
        txtWelcomeFreeEmployees = findViewById(R.id.txt_welcome_free_employees);
        txtWelcomeFreeNoImport = findViewById(R.id.txt_welcome_free_no_import);
        txtWelcomeFreeNoLogo = findViewById(R.id.txt_welcome_free_no_logo);
        txtWelcomeFreeConsiderPro = findViewById(R.id.txt_welcome_free_consider_pro);


        txtWelcomeGuestDesc.setText(HtmlCompat.fromHtml(getString(R.string.welcome_guest_desc), HtmlCompat.FROM_HTML_MODE_LEGACY));
        txtWelcomeAdminDescLogin.setText(HtmlCompat.fromHtml(getString(R.string.welcome_admin_desc_login), HtmlCompat.FROM_HTML_MODE_LEGACY));
        txtWelcomeAdminDescCreds.setText(HtmlCompat.fromHtml(getString(R.string.welcome_admin_desc_creds), HtmlCompat.FROM_HTML_MODE_LEGACY));
        txtWelcomeAdminDescPass.setText(HtmlCompat.fromHtml(getString(R.string.welcome_admin_desc_pass), HtmlCompat.FROM_HTML_MODE_LEGACY));
        txtWelcomeFreeEmployees.setText(HtmlCompat.fromHtml(getString(R.string.welcome_free_employees), HtmlCompat.FROM_HTML_MODE_LEGACY));
        txtWelcomeFreeNoImport.setText(HtmlCompat.fromHtml(getString(R.string.welcome_free_no_import), HtmlCompat.FROM_HTML_MODE_LEGACY));
        txtWelcomeFreeNoLogo.setText(HtmlCompat.fromHtml(getString(R.string.welcome_free_no_logo), HtmlCompat.FROM_HTML_MODE_LEGACY));
        txtWelcomeFreeConsiderPro.setText(HtmlCompat.fromHtml(getString(R.string.welcome_free_consider_pro), HtmlCompat.FROM_HTML_MODE_LEGACY));


        btnContinueWelcome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkboxDontShowAgain.isChecked()) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean(KEY_WELCOME_SCREEN_SHOWN_ONCE, true);
                    editor.apply();
                }
                navigateToMainApp();
            }
        });
    }

    private void navigateToMainApp() {
        Intent intent = new Intent(WelcomeActivity.this, MainHandwashing.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Ensure MainHandwashing is the new root
        startActivity(intent);
        finish(); // Finish WelcomeActivity so user can't navigate back to it
    }

    @Override
    public void onBackPressed() {
        // To prevent users from simply pressing back to skip it on the very first launch
        // you could make it behave like the continue button.
        // Or, allow back press if you prefer. For a one-time info screen,
        // acting like "continue" on back press might be user-friendly.
        if (btnContinueWelcome != null) {
            btnContinueWelcome.performClick();
        } else {
            super.onBackPressed();
        }
    }
}