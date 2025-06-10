package com.jarindimick.handwashtracking.gui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.text.HtmlCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.jarindimick.handwashtracking.R;

public class WelcomeActivity extends AppCompatActivity {

    public static final String PREFS_WELCOME_FILE = "WelcomeScreenPrefs";
    public static final String KEY_WELCOME_SCREEN_SHOWN_ONCE = "welcomeScreenShownOnce";

    private CheckBox checkboxDontShowAgain;
    private Button btnContinueWelcome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        SharedPreferences prefs = getSharedPreferences(PREFS_WELCOME_FILE, MODE_PRIVATE);
        if (prefs.getBoolean(KEY_WELCOME_SCREEN_SHOWN_ONCE, false)) {
            navigateToMainApp();
            return;
        }

        setContentView(R.layout.activity_welcome);

        // Use the ID you have in your root layout
        View rootView = findViewById(R.id.welcome_activity_root);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        checkboxDontShowAgain = findViewById(R.id.checkbox_dont_show_welcome);
        btnContinueWelcome = findViewById(R.id.btn_continue_welcome);

        // Find all TextViews by their ID from the (now corrected) XML layout
        TextView tvWelcomeTitle = findViewById(R.id.welcome_title);
        TextView tvWelcomeIntro = findViewById(R.id.welcome_intro);
        TextView tvGuestTitle = findViewById(R.id.welcome_guest_title);
        TextView tvAdminTitle = findViewById(R.id.welcome_admin_title);
        TextView tvFreeVersionTitle = findViewById(R.id.welcome_free_version_title);
        TextView txtWelcomeGuestDesc = findViewById(R.id.txt_welcome_guest_desc);
        TextView txtWelcomeAdminDescLogin = findViewById(R.id.txt_welcome_admin_desc_login);
        TextView txtWelcomeAdminDescCreds = findViewById(R.id.txt_welcome_admin_desc_creds);
        TextView txtWelcomeAdminDescPass = findViewById(R.id.txt_welcome_admin_desc_pass);
        TextView txtWelcomeFreeEmployees = findViewById(R.id.txt_welcome_free_employees);
        TextView txtWelcomeFreeNoImport = findViewById(R.id.txt_welcome_free_no_import);
        TextView txtWelcomeFreeNoLogo = findViewById(R.id.txt_welcome_free_no_logo);
        TextView txtWelcomeFreeConsiderPro = findViewById(R.id.txt_welcome_free_consider_pro);

        // Set text for all TextViews by loading from strings.xml
        tvWelcomeTitle.setText(HtmlCompat.fromHtml(getString(R.string.welcome_title), HtmlCompat.FROM_HTML_MODE_LEGACY));
        tvWelcomeIntro.setText(HtmlCompat.fromHtml(getString(R.string.welcome_intro), HtmlCompat.FROM_HTML_MODE_LEGACY));
        tvGuestTitle.setText(HtmlCompat.fromHtml(getString(R.string.welcome_guest_title), HtmlCompat.FROM_HTML_MODE_LEGACY));
        tvAdminTitle.setText(HtmlCompat.fromHtml(getString(R.string.welcome_admin_title), HtmlCompat.FROM_HTML_MODE_LEGACY));
        tvFreeVersionTitle.setText(HtmlCompat.fromHtml(getString(R.string.welcome_free_version_title), HtmlCompat.FROM_HTML_MODE_LEGACY));
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
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (btnContinueWelcome != null) {
            btnContinueWelcome.performClick();
        } else {
            navigateToMainApp();
        }
    }
}