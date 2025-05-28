package com.jarindimick.handwashtracking.gui;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log; // Added for debugging

import androidx.appcompat.app.AppCompatActivity;

import com.jarindimick.handwashtracking.R;

import java.util.Locale;

public class DryHandsActivity extends AppCompatActivity {

    private String employeeNumber;

    // Updated Timings
    private static final long STEP_DURATION_MS = 5000; // 5 seconds for Dry Hands
    private static final long TOTAL_PROCESS_DURATION_MS = 37000; // Total for Wet, Soap, Rinse, Dry

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
        setContentView(R.layout.activity_dry_hands);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        employeeNumber = getIntent().getStringExtra("employee_number");
        // Get remaining overall time. Fallback if not passed, assuming this is the last step of the 37s cycle.
        overallTimeRemainingMs = getIntent().getLongExtra("overall_time_remaining", STEP_DURATION_MS);

        // Initialize UI Elements
        progressStepTimer = findViewById(R.id.progress_step_timer);
        textStepTimerValue = findViewById(R.id.text_step_timer_value);
        progressOverallTimer = findViewById(R.id.progress_overall_timer);
        textOverallTimerValue = findViewById(R.id.text_overall_timer_value);
        imageStepAnimation = findViewById(R.id.image_step_animation);

        // Set correct animation/image for this step
        imageStepAnimation.setImageResource(R.drawable.ic_dry_hands_anim);

        // Configure ProgressBars Max and initial Text
        progressStepTimer.setMax((int) STEP_DURATION_MS);
        textStepTimerValue.setText(String.format(Locale.getDefault(), "%ds", STEP_DURATION_MS / 1000));

        progressOverallTimer.setMax((int) TOTAL_PROCESS_DURATION_MS);
        // Set initial overall timer text based on what was passed
        textOverallTimerValue.setText(String.format(Locale.getDefault(), "%ds", overallTimeRemainingMs / 1000 + (overallTimeRemainingMs % 1000 > 0 ? 1 : 0)));


        Log.d("DryHandsActivity", "onCreate: Employee No: " + employeeNumber + ", Overall Time Remaining: " + overallTimeRemainingMs);

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
        // Ensure the overall timer only counts down what's truly remaining for this last step
        // The 'overallTimeRemainingMs' should be roughly equal to STEP_DURATION_MS if passed correctly
        progressOverallTimer.setProgress((int) overallTimeRemainingMs);
        overallCountDownTimer = new CountDownTimer(overallTimeRemainingMs, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                overallTimeRemainingMs = millisUntilFinished;
                progressOverallTimer.setProgress((int) millisUntilFinished);
                textOverallTimerValue.setText(String.format(Locale.getDefault(), "%ds", millisUntilFinished / 1000 + (millisUntilFinished % 1000 > 0 ? 1 : 0)));
            }

            @Override
            public void onFinish() {
                progressOverallTimer.setProgress(0);
                textOverallTimerValue.setText("0s");
                // This is the last of the *timed* washing steps.
                Toast.makeText(DryHandsActivity.this, "Handwashing Steps Complete!", Toast.LENGTH_SHORT).show();
                Log.d("DryHandsActivity", "Overall timer finished.");
            }
        }.start();
    }

    private void proceedToNextStep() {
        Log.d("DryHandsActivity", "Proceeding to ConfirmHandwashActivity.");
        // This is the last timed step, so go to ConfirmHandwashActivity
        Intent intent = new Intent(DryHandsActivity.this, ConfirmHandwashActivity.class);
        intent.putExtra("employee_number", employeeNumber);
        // No need to pass overall_time_remaining as the timed sequence is done.
        // ConfirmHandwashActivity has its own delay for camera.
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
        Log.d("DryHandsActivity", "onDestroy called, timers cancelled.");
    }
}