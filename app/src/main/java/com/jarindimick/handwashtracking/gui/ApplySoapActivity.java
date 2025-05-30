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
// Toast import is fine if you plan to use it, otherwise optional
// import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.jarindimick.handwashtracking.R;

import java.util.Locale;

public class ApplySoapActivity extends AppCompatActivity {

    private String employeeNumber;

    // Timings for Apply Soap & Scrub
    private static final long STEP_DURATION_MS = 20000; // 20 seconds for Apply Soap & Scrub
    private static final long TOTAL_PROCESS_DURATION_MS = 34000; //

    // UI Elements
    private ProgressBar progressStepTimer;
    private TextView textStepTimerValue;
    private ProgressBar progressOverallTimer;
    private TextView textOverallTimerValue;
    // private ImageView imageStepAnimation; // OLD
    private VideoView videoStepView;      // NEW

    // Timers
    private CountDownTimer stepCountDownTimer;
    private CountDownTimer overallCountDownTimer;
    private long overallTimeRemainingMs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apply_soap); //
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //
        if (getSupportActionBar() != null) getSupportActionBar().hide(); //

        employeeNumber = getIntent().getStringExtra("employee_number"); //
        overallTimeRemainingMs = getIntent().getLongExtra("overall_time_remaining", TOTAL_PROCESS_DURATION_MS - 5000); //

        progressStepTimer = findViewById(R.id.progress_step_timer); //
        textStepTimerValue = findViewById(R.id.text_step_timer_value); //
        progressOverallTimer = findViewById(R.id.progress_overall_timer); //
        textOverallTimerValue = findViewById(R.id.text_overall_timer_value); //
        videoStepView = findViewById(R.id.video_step_animation); // Ensure this ID matches your XML

        // --- SETUP VIDEO PLAYBACK ---
        if (videoStepView != null) {
            // CRITICAL: Replace 'apply_soap_instruction_video' with your actual video file name in res/raw
            String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.apply_soap_instruction_video;
            Uri videoUri = Uri.parse(videoPath);
            videoStepView.setVideoURI(videoUri);

            videoStepView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.setVolume(0f, 0f); // Mute the video
                    mp.start(); // Start playing

                    // Loop if video is shorter than step duration (20 seconds for this step)
                    if (mp.getDuration() > 0 && mp.getDuration() < STEP_DURATION_MS) {
                        mp.setLooping(true);
                        Log.d("ApplySoapActivity", "Video shorter, enabling loop. Duration: " + mp.getDuration() + "ms");
                    } else {
                        mp.setLooping(false);
                        Log.d("ApplySoapActivity", "Video not looping. Duration: " + mp.getDuration() + "ms");
                    }
                }
            });

            videoStepView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Log.e("ApplySoapActivity", "VideoView Error: what=" + what + ", extra=" + extra);
                    return true; // Error handled
                }
            });
        }
        // --- END OF VIDEO PLAYBACK SETUP ---

        progressStepTimer.setMax((int) STEP_DURATION_MS); //
        textStepTimerValue.setText(String.format(Locale.getDefault(), "%ds", STEP_DURATION_MS / 1000)); //

        progressOverallTimer.setMax((int) TOTAL_PROCESS_DURATION_MS); //
        textOverallTimerValue.setText(String.format(Locale.getDefault(), "%ds", overallTimeRemainingMs / 1000 + (overallTimeRemainingMs % 1000 > 0 ? 1 : 0))); //

        Log.d("ApplySoapActivity", "onCreate: Employee No: " + employeeNumber + ", Overall Time Remaining: " + overallTimeRemainingMs); //

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
                Log.d("ApplySoapActivity", "Overall timer portion for this step finished."); //
            }
        }.start();
    }

    private void proceedToNextStep() {
        Log.d("ApplySoapActivity", "Proceeding to RinseHandsActivity. Overall time remaining: " + overallTimeRemainingMs); //
        Intent intent = new Intent(ApplySoapActivity.this, RinseHandsActivity.class); //
        intent.putExtra("employee_number", employeeNumber); //
        intent.putExtra("overall_time_remaining", overallTimeRemainingMs); //
        startActivity(intent); //
        finish(); //
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoStepView != null && videoStepView.isPlaying()) {
            videoStepView.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (videoStepView != null && !videoStepView.isPlaying()) {
            // CRITICAL: Replace 'apply_soap_instruction_video' with your actual video file name
            String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.apply_soap_instruction_video;
            Uri videoUri = Uri.parse(videoPath);
            videoStepView.setVideoURI(videoUri);
            // The onPreparedListener will handle starting, muting, and looping
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoStepView != null) {
            videoStepView.stopPlayback();
        }
        if (stepCountDownTimer != null) {
            stepCountDownTimer.cancel(); //
        }
        if (overallCountDownTimer != null) {
            overallCountDownTimer.cancel(); //
        }
        Log.d("ApplySoapActivity", "onDestroy called, timers cancelled, video stopped."); //
    }
}