package com.jarindimick.handwashtracking.gui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Spanned; // For styled text
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.text.HtmlCompat; // For better HTML parsing
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.jarindimick.handwashtracking.R;

public class WelcomeActivity extends AppCompatActivity {

    // Constants for SharedPreferences
    public static final String PREFS_WELCOME_FILE = "WelcomeScreenPrefs";
    public static final String KEY_WELCOME_SCREEN_SHOWN_ONCE = "welcomeScreenShownOnce";

    private CheckBox checkboxDontShowAgain;
    private Button btnContinueWelcome;

    // TextViews for the descriptions
    private TextView txtWelcomeGuestDesc, txtWelcomeAdminDescLogin, txtWelcomeAdminDescCreds,
            txtWelcomeAdminDescPass, txtWelcomeFreeEmployees, txtWelcomeFreeNoImport,
            txtWelcomeFreeNoLogo, txtWelcomeFreeConsiderPro;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // Call super.onCreate first
        EdgeToEdge.enable(this); // Enable EdgeToEdge

        // Check if the welcome screen should be skipped
        SharedPreferences prefs = getSharedPreferences(PREFS_WELCOME_FILE, MODE_PRIVATE);
        if (prefs.getBoolean(KEY_WELCOME_SCREEN_SHOWN_ONCE, false)) {
            navigateToMainApp();
            return; // Important: Do not proceed to setContentView for this activity if already shown
        }

        setContentView(R.layout.activity_welcome);

        // Apply window insets to the root view of activity_welcome.xml
        // Assuming your root view in activity_welcome.xml has android:id="@+id/welcome_activity_root"
        // or use findViewById(android.R.id.content).getRootView() if no specific root ID
        View rootView = findViewById(android.R.id.content).getRootView();
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Apply the insets as padding to the view. This is more straightforward.
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED; // Indicate insets are consumed
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize views
        checkboxDontShowAgain = findViewById(R.id.checkbox_dont_show_welcome);
        btnContinueWelcome = findViewById(R.id.btn_continue_welcome);

        // Initialize and set text for all descriptive TextViews using HtmlCompat
        txtWelcomeGuestDesc = findViewById(R.id.txt_welcome_guest_desc);
        txtWelcomeAdminDescLogin = findViewById(R.id.txt_welcome_admin_desc_login);
        txtWelcomeAdminDescCreds = findViewById(R.id.txt_welcome_admin_desc_creds);
        txtWelcomeAdminDescPass = findViewById(R.id.txt_welcome_admin_desc_pass);
        txtWelcomeFreeEmployees = findViewById(R.id.txt_welcome_free_employees);
        txtWelcomeFreeNoImport = findViewById(R.id.txt_welcome_free_no_import);
        txtWelcomeFreeNoLogo = findViewById(R.id.txt_welcome_free_no_logo);
        txtWelcomeFreeConsiderPro = findViewById(R.id.txt_welcome_free_consider_pro);

        // Set text using HtmlCompat to render HTML (like <b> tags) from strings.xml
        txtWelcomeGuestDesc.setText(HtmlCompat.fromHtml(getString(R.string.welcome_guest_desc), HtmlCompat.FROM_HTML_MODE_LEGACY));
        txtWelcomeAdminDescLogin.setText(HtmlCompat.fromHtml(getString(R.string.welcome_admin_desc_login), HtmlCompat.FROM_HTML_MODE_LEGACY));
        txtWelcomeAdminDescCreds.setText(HtmlCompat.fromHtml(getString(R.string.welcome_admin_desc_creds), HtmlCompat.FROM_HTML_MODE_LEGACY));
        txtWelcomeAdminDescPass.setText(HtmlCompat.fromHtml(getString(R.string.welcome_admin_desc_pass), HtmlCompat.FROM_HTML_MODE_LEGACY));
        txtWelcomeFreeEmployees.setText(HtmlCompat.fromHtml(getString(R.string.welcome_free_employees), HtmlCompat.FROM_HTML_MODE_LEGACY));
        txtWelcomeFreeNoImport.setText(HtmlCompat.fromHtml(getString(R.string.welcome_free_no_import), HtmlCompat.FROM_HTML_MODE_LEGACY));
        txtWelcomeFreeNoLogo.setText(HtmlCompat.fromHtml(getString(R.string.welcome_free_no_logo), HtmlCompat.FROM_HTML_MODE_LEGACY));
        txtWelcomeFreeConsiderPro.setText(HtmlCompat.fromHtml(getString(R.string.welcome_free_consider_pro), HtmlCompat.FROM_HTML_MODE_LEGACY));


        btnContinueWelcome.setOnClickListener(v -> {
            if (checkboxDontShowAgain.isChecked()) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(KEY_WELCOME_SCREEN_SHOWN_ONCE, true);
                editor.apply();
            }
            navigateToMainApp();
        });
    }

    private void navigateToMainApp() {
        Intent intent = new Intent(WelcomeActivity.this, MainHandwashing.class);
        // Add flags to make MainHandwashing the new root and clear WelcomeActivity from backstack
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Finish WelcomeActivity so user can't navigate back to it
    }

    @Override
    public void onBackPressed() {
        // Make back press behave like continue, effectively not allowing user to easily skip
        // the welcome screen on first launch if they try to use back.
        if (btnContinueWelcome != null) {
            btnContinueWelcome.performClick();
        } else {
            // Fallback if button is somehow null, though unlikely
            navigateToMainApp();
        }
        // Do not call super.onBackPressed(); to prevent normal back navigation from this screen
    }
}