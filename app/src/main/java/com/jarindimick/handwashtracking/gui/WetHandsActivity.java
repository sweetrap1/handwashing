package com.jarindimick.handwashtracking.gui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler; // Import Handler
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.jarindimick.handwashtracking.R;

public class WetHandsActivity extends AppCompatActivity {

    private String employeeNumber;
    private static final long STEP_DELAY_MS = 1000; // 1 second delay for testing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wet_hands);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        employeeNumber = getIntent().getStringExtra("employee_number");

        // No more button, transition automatically
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(WetHandsActivity.this, ApplySoapActivity.class);
                intent.putExtra("employee_number", employeeNumber);
                startActivity(intent);
                finish(); // Close this activity
            }
        }, STEP_DELAY_MS);
    }
}