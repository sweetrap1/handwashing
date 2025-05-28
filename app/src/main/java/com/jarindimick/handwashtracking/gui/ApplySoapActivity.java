package com.jarindimick.handwashtracking.gui;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast; // Included, though not explicitly used in this version's timers
import android.util.Log;    // Added for debugging
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.jarindimick.handwashtracking.R;

import java.util.Locale;

public class ApplySoapActivity extends AppCompatActivity {

    private String employeeNumber;

    // Updated Timings
    private static final long STEP_DURATION_MS = 20000; // 20 seconds for Apply Soap & Scrub
    private static final long TOTAL_PROCESS_DURATION_MS = 37000; // Total for Wet(5) + Soap(20) + Rinse(7) + Dry(5)

    // UI Elements
    private ProgressBar progressStepTimer;
    private TextView textStepTimerValue;
    private ProgressBar progressOverallTimer;
    private TextView textOverallTimerValue;
    private ImageView imageStepAnimation;

    // Timers
    private CountDownTimer stepCountDownTimer;
    private CountDownTimer overallCountDownTimer;
    private long overallTimeRemainingMs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ensure you are using the layout file that has the centered LinearLayout structure
        setContentView(R.layout.activity_apply_soap);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        employeeNumber = getIntent().getStringExtra("employee_number");
        // Get remaining overall time. Fallback calculates based on Wet Hands step (5s)
        // TOTAL_PROCESS_DURATION_MS - 5000 (WetHands) = 32000
        overallTimeRemainingMs = getIntent().getLongExtra("overall_time_remaining", TOTAL_PROCESS_DURATION_MS - 5000);

        // Initialize UI Elements
        progressStepTimer = findViewById(R.id.progress_step_timer);
        textStepTimerValue = findViewById(R.id.text_step_timer_value);
        progressOverallTimer = findViewById(R.id.progress_overall_timer);
        textOverallTimerValue = findViewById(R.id.text_overall_timer_value);
        imageStepAnimation = findViewById(R.id.image_step_animation);

        // Set correct animation/image for this step
        imageStepAnimation.setImageResource(R.drawable.ic_apply_soap_anim);

        // Configure ProgressBars Max and initial Text
        progressStepTimer.setMax((int) STEP_DURATION_MS);
        textStepTimerValue.setText(String.format(Locale.getDefault(), "%ds", STEP_DURATION_MS / 1000));

        progressOverallTimer.setMax((int) TOTAL_PROCESS_DURATION_MS);
        // Set initial overall timer text based on what was passed
        textOverallTimerValue.setText(String.format(Locale.getDefault(), "%ds", overallTimeRemainingMs / 1000 + (overallTimeRemainingMs % 1000 > 0 ? 1 : 0)));

        Log.d("ApplySoapActivity", "onCreate: Employee No: " + employeeNumber + ", Overall Time Remaining: " + overallTimeRemainingMs);

        startStepTimer();
        startOverallTimer();
    }

    private void startStepTimer() {
        progressStepTimer.setProgress((int) STEP_DURATION_MS); // Start full
        stepCountDownTimer = new CountDownTimer(STEP_DURATION_MS, 50) { // Update UI frequently
            @Override
            public void onTick(long millisUntilFinished) {
                progressStepTimer.setProgress((int) millisUntilFinished);
                textStepTimerValue.setText(String.format(Locale.getDefault(), "%ds", millisUntilFinished / 1000 + (millisUntilFinished % 1000 > 0 ? 1 : 0)));
            }

            @Override
            public void onFinish() {
                progressStepTimer.setProgress(0);
                textStepTimerValue.setText("0s");
                proceedToNextStep();
            }
        }.start();
    }

    private void startOverallTimer() {
        progressOverallTimer.setProgress((int) overallTimeRemainingMs);
        overallCountDownTimer = new CountDownTimer(overallTimeRemainingMs, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                overallTimeRemainingMs = millisUntilFinished; // Update the class field
                progressOverallTimer.setProgress((int) millisUntilFinished);
                textOverallTimerValue.setText(String.format(Locale.getDefault(), "%ds", millisUntilFinished / 1000 + (millisUntilFinished % 1000 > 0 ? 1 : 0)));
            }

            @Override
            public void onFinish() {
                progressOverallTimer.setProgress(0);
                textOverallTimerValue.setText("0s");
                // This isn't the final step of the overall process, so no specific message here.
                Log.d("ApplySoapActivity", "Overall timer portion for this step finished.");
            }
        }.start();
    }

    private void proceedToNextStep() {
        Log.d("ApplySoapActivity", "Proceeding to RinseHandsActivity. Overall time remaining: " + overallTimeRemainingMs);
        Intent intent = new Intent(ApplySoapActivity.this, RinseHandsActivity.class);
        intent.putExtra("employee_number", employeeNumber);
        intent.putExtra("overall_time_remaining", overallTimeRemainingMs); // Pass the updated remaining time
        startActivity(intent);
        finish(); // Close this activity
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (stepCountDownTimer != null) {
            stepCountDownTimer.cancel();
        }
        if (overallCountDownTimer != null) {
            overallCountDownTimer.cancel();
        }
        Log.d("ApplySoapActivity", "onDestroy called, timers cancelled.");
    }
}