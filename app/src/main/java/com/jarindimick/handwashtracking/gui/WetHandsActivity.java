package com.jarindimick.handwashtracking.gui;

import android.content.Intent;
import android.media.MediaPlayer; // Added import
import android.net.Uri;           // Added import
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;   // Added import

import androidx.appcompat.app.AppCompatActivity;

import com.jarindimick.handwashtracking.R;

import java.util.Locale;

public class WetHandsActivity extends AppCompatActivity {

    private String employeeNumber;

    private static final long STEP_DURATION_MS = 4000; // 5 seconds for Wet Hands
    public static final long TOTAL_PROCESS_DURATION_MS = 35000; // Total

    private ProgressBar progressStepTimer;
    private TextView textStepTimerValue;
    private ProgressBar progressOverallTimer;
    private TextView textOverallTimerValue;
    // private ImageView imageStepAnimation; // OLD - Commented out or removed
    private VideoView videoStepView;      // NEW - For VideoView

    private CountDownTimer stepCountDownTimer;
    private CountDownTimer overallCountDownTimer;
    private long overallTimeRemainingMs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wet_hands); //
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //
        if (getSupportActionBar() != null) getSupportActionBar().hide(); //

        employeeNumber = getIntent().getStringExtra("employee_number"); //
        overallTimeRemainingMs = getIntent().getLongExtra("overall_time_remaining", TOTAL_PROCESS_DURATION_MS); //

        progressStepTimer = findViewById(R.id.progress_step_timer); //
        textStepTimerValue = findViewById(R.id.text_step_timer_value); //
        progressOverallTimer = findViewById(R.id.progress_overall_timer); //
        textOverallTimerValue = findViewById(R.id.text_overall_timer_value); //
        videoStepView = findViewById(R.id.video_step_animation); // NEW - Get VideoView by its new ID

        // Inside WetHandsActivity.java

        // --- SETUP VIDEO PLAYBACK ---
        if (videoStepView != null) {
            String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.wet_hands_instruction_video;
            // ^^ IMPORTANT: Replace 'wet_hands_instruction_video' with your video file name.
            Uri videoUri = Uri.parse(videoPath);
            videoStepView.setVideoURI(videoUri);

            videoStepView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    // --- ADD THIS LINE TO MUTE THE VIDEO ---
                    mp.setVolume(0f, 0f);
                    // --- END OF LINE TO MUTE ---

                    mp.start(); // Start playing

                    if (mp.getDuration() > 0 && mp.getDuration() < STEP_DURATION_MS) {
                        mp.setLooping(true);
                        Log.d("WetHandsActivity", "Video shorter than step, enabling loop. Video duration: " + mp.getDuration() + "ms");
                    } else {
                        mp.setLooping(false);
                        Log.d("WetHandsActivity", "Video not looping. Video duration: " + mp.getDuration() + "ms");
                    }
                }
            });

            videoStepView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Log.e("WetHandsActivity", "VideoView Error: what=" + what + ", extra=" + extra);
                    return true;
                }
            });
        }
        // --- END OF VIDEO PLAYBACK SETUP ---
        progressStepTimer.setMax((int) STEP_DURATION_MS); //
        textStepTimerValue.setText(String.format(Locale.getDefault(), "%ds", STEP_DURATION_MS / 1000)); //

        progressOverallTimer.setMax((int) TOTAL_PROCESS_DURATION_MS); //
        textOverallTimerValue.setText(String.format(Locale.getDefault(), "%ds", overallTimeRemainingMs / 1000 + (overallTimeRemainingMs % 1000 > 0 ? 1 : 0))); //

        Log.d("WetHandsActivity", "onCreate: Employee No: " + employeeNumber + ", Overall Time Remaining: " + overallTimeRemainingMs); //

        startStepTimer(); //
        startOverallTimer(); //
    }

    private void startStepTimer() {
        progressStepTimer.setProgress((int) STEP_DURATION_MS); //
        stepCountDownTimer = new CountDownTimer(STEP_DURATION_MS, 50) { //
            @Override
            public void onTick(long millisUntilFinished) {
                progressStepTimer.setProgress((int) millisUntilFinished); //
                textStepTimerValue.setText(String.format(Locale.getDefault(), "%ds", millisUntilFinished / 1000 + (millisUntilFinished % 1000 > 0 ? 1 : 0))); //
            }

            @Override
            public void onFinish() {
                progressStepTimer.setProgress(0); //
                textStepTimerValue.setText("0s"); //
                proceedToNextStep(); //
            }
        }.start();
    }

    private void startOverallTimer() {
        progressOverallTimer.setProgress((int) overallTimeRemainingMs); //
        overallCountDownTimer = new CountDownTimer(overallTimeRemainingMs, 100) { //
            @Override
            public void onTick(long millisUntilFinished) {
                overallTimeRemainingMs = millisUntilFinished; //
                progressOverallTimer.setProgress((int) millisUntilFinished); //
                textOverallTimerValue.setText(String.format(Locale.getDefault(), "%ds", millisUntilFinished / 1000 + (millisUntilFinished % 1000 > 0 ? 1 : 0))); //
            }

            @Override
            public void onFinish() {
                progressOverallTimer.setProgress(0); //
                textOverallTimerValue.setText("0s"); //
                Log.d("WetHandsActivity", "Overall timer portion for this step finished."); //
            }
        }.start();
    }

    private void proceedToNextStep() {
        Log.d("WetHandsActivity", "Proceeding to ApplySoapActivity. Overall time remaining: " + overallTimeRemainingMs); //
        Intent intent = new Intent(WetHandsActivity.this, ApplySoapActivity.class); //
        intent.putExtra("employee_number", employeeNumber); //
        intent.putExtra("overall_time_remaining", overallTimeRemainingMs); //
        startActivity(intent); //
        finish(); //
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoStepView != null && videoStepView.isPlaying()) {
            videoStepView.pause(); // Pause video when activity is paused
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (videoStepView != null && !videoStepView.isPlaying()) {
            // Resume video playback if it was paused.
            // This also handles the initial start if setOnPreparedListener didn't trigger a start
            // due to activity lifecycle. However, our primary start is in onPrepared.
            String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.wet_hands_instruction_video;
            Uri videoUri = Uri.parse(videoPath);
            videoStepView.setVideoURI(videoUri); // Re-set URI as VideoView might lose state
            // The onPreparedListener will handle starting and looping
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoStepView != null) {
            videoStepView.stopPlayback(); // Stop video and release resources
        }
        if (stepCountDownTimer != null) {
            stepCountDownTimer.cancel(); //
        }
        if (overallCountDownTimer != null) {
            overallCountDownTimer.cancel(); //
        }
        Log.d("WetHandsActivity", "onDestroy called, timers cancelled, video stopped."); //
    }
}