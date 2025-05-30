package com.jarindimick.handwashtracking.gui;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.jarindimick.handwashtracking.R;

import java.util.Locale;

public class DryHandsActivity extends AppCompatActivity {

    private String employeeNumber;

    private static final long STEP_DURATION_MS = 5000; // 5 seconds for Dry Hands
    private static final long TOTAL_PROCESS_DURATION_MS = 37000; //

    private ProgressBar progressStepTimer;
    private TextView textStepTimerValue;
    private ProgressBar progressOverallTimer;
    private TextView textOverallTimerValue;
    private VideoView videoStepView;

    private CountDownTimer stepCountDownTimer;
    private CountDownTimer overallCountDownTimer;
    private long overallTimeRemainingMs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dry_hands); //
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //
        if (getSupportActionBar() != null) getSupportActionBar().hide(); //

        employeeNumber = getIntent().getStringExtra("employee_number"); //
        overallTimeRemainingMs = getIntent().getLongExtra("overall_time_remaining", STEP_DURATION_MS); //

        progressStepTimer = findViewById(R.id.progress_step_timer); //
        textStepTimerValue = findViewById(R.id.text_step_timer_value); //
        progressOverallTimer = findViewById(R.id.progress_overall_timer); //
        textOverallTimerValue = findViewById(R.id.text_overall_timer_value); //
        videoStepView = findViewById(R.id.video_step_animation);

        if (videoStepView != null) {
            String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.dry_hands_instruction_video;
            Uri videoUri = Uri.parse(videoPath);
            videoStepView.setVideoURI(videoUri);

            videoStepView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.setVolume(0f, 0f);
                    mp.start();

                    if (mp.getDuration() > 0 && mp.getDuration() < STEP_DURATION_MS) {
                        mp.setLooping(true);
                        Log.d("DryHandsActivity", "Video shorter, enabling loop. Duration: " + mp.getDuration() + "ms");
                    } else {
                        mp.setLooping(false);
                        Log.d("DryHandsActivity", "Video not looping. Duration: " + mp.getDuration() + "ms");
                    }
                }
            });

            videoStepView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Log.e("DryHandsActivity", "VideoView Error: what=" + what + ", extra=" + extra);
                    return true;
                }
            });
        }

        progressStepTimer.setMax((int) STEP_DURATION_MS); //
        textStepTimerValue.setText(String.format(Locale.getDefault(), "%ds", STEP_DURATION_MS / 1000)); //
        progressOverallTimer.setMax((int) TOTAL_PROCESS_DURATION_MS); //
        textOverallTimerValue.setText(String.format(Locale.getDefault(), "%ds", overallTimeRemainingMs / 1000 + (overallTimeRemainingMs % 1000 > 0 ? 1 : 0))); //
        Log.d("DryHandsActivity", "onCreate: Employee No: " + employeeNumber + ", Overall Time Remaining: " + overallTimeRemainingMs); //

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
                Toast.makeText(DryHandsActivity.this, "Handwashing Steps Complete!", Toast.LENGTH_SHORT).show(); //
                Log.d("DryHandsActivity", "Overall timer finished."); //
            }
        }.start();
    }

    private void proceedToNextStep() {
        Log.d("DryHandsActivity", "Proceeding to ConfirmHandwashActivity."); //
        Intent intent = new Intent(DryHandsActivity.this, ConfirmHandwashActivity.class); //
        intent.putExtra("employee_number", employeeNumber); //
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
            String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.dry_hands_instruction_video;
            Uri videoUri = Uri.parse(videoPath);
            videoStepView.setVideoURI(videoUri);
            // onPrepared will handle start and loop
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
        Log.d("DryHandsActivity", "onDestroy called, timers cancelled, video stopped."); //
    }
}