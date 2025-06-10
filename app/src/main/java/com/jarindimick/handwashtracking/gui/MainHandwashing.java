package com.jarindimick.handwashtracking.gui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.text.HtmlCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.jarindimick.handwashtracking.R;
import com.jarindimick.handwashtracking.databasehelper.DatabaseHelper;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainHandwashing extends AppCompatActivity {

    private ImageView img_custom_logo;
    private TextView txt_datetime;
    private EditText edit_employee_number;
    private Button btn_start;
    private TableLayout table_top_handwashers;
    private final Handler timeUpdateHandler = new Handler(Looper.getMainLooper());
    private Runnable updateTimeRunnable;
    private DatabaseHelper dbHelper;
    private Toolbar mainToolbar;

    // Executor for background tasks
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    public static final String PREFS_MAIN_TOUR_FILE = "MainTourPrefs";
    public static final String KEY_MAIN_INTERACTIVE_TOUR_SHOWN = "mainInteractiveTourShown";

    private boolean hasWindowFocusForTour = false;
    private boolean interactiveTourAttemptedThisSession = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_handwashing);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return windowInsets;
        });

        setupgui();

        if (mainToolbar != null) {
            setSupportActionBar(mainToolbar);
        }

        setupListeners();
        dbHelper = new DatabaseHelper(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && !interactiveTourAttemptedThisSession) {
            hasWindowFocusForTour = true;
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (hasWindowFocusForTour) {
                    showInteractiveTourIfNeeded();
                }
            }, 300);
        } else if (!hasFocus) {
            hasWindowFocusForTour = false;
        }
    }

    private void showInteractiveTourIfNeeded() {
        final SharedPreferences tourPrefs = getSharedPreferences(PREFS_MAIN_TOUR_FILE, MODE_PRIVATE);
        boolean tourShown = tourPrefs.getBoolean(KEY_MAIN_INTERACTIVE_TOUR_SHOWN, false);

        if (tourShown || interactiveTourAttemptedThisSession) {
            return;
        }

        interactiveTourAttemptedThisSession = true;

        if (edit_employee_number != null && mainToolbar != null && mainToolbar.getMenu() != null && mainToolbar.getMenu().findItem(R.id.menu_admin_login) != null) {
            String adminTitle = "Admin Features";
            String adminDescription = "Access employee management & app settings. Default login/password: admin (Please Change)).";

            TapTargetSequence.Listener sequenceListener = new TapTargetSequence.Listener() {
                @Override
                public void onSequenceFinish() {
                    showFreeVersionLimitationsDialog(() -> {
                        SharedPreferences.Editor editor = tourPrefs.edit();
                        editor.putBoolean(KEY_MAIN_INTERACTIVE_TOUR_SHOWN, true);
                        editor.apply();
                    });
                }

                @Override
                public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {}

                @Override
                public void onSequenceCanceled(TapTarget lastTarget) {
                    showFreeVersionLimitationsDialog(() -> {
                        SharedPreferences.Editor editor = tourPrefs.edit();
                        editor.putBoolean(KEY_MAIN_INTERACTIVE_TOUR_SHOWN, true);
                        editor.apply();
                    });
                }
            };

            ArrayList<TapTarget> targets = new ArrayList<>();
            targets.add(TapTarget.forView(edit_employee_number, "Guest User", "Enter employee 0 to log a wash if you are not registered.")
                    .outerCircleColor(R.color.purple_500).outerCircleAlpha(0.75f).targetCircleColor(android.R.color.white)
                    .titleTextColor(android.R.color.white).descriptionTextColor(android.R.color.white)
                    .textTypeface(Typeface.SANS_SERIF).dimColor(R.color.tour_dim_background).drawShadow(true)
                    .cancelable(false).targetRadius(20).id(1));

            try {
                targets.add(TapTarget.forToolbarOverflow(mainToolbar, adminTitle, adminDescription)
                        .outerCircleColor(R.color.purple_500).outerCircleAlpha(0.75f).targetCircleColor(android.R.color.white)
                        .titleTextColor(android.R.color.white).descriptionTextColor(android.R.color.white)
                        .textTypeface(Typeface.SANS_SERIF).dimColor(R.color.tour_dim_background).drawShadow(true)
                        .cancelable(false).targetRadius(20).id(2));
            } catch (Exception e) {
                Log.e("TapTarget", "Failed to create TapTarget for toolbar overflow.", e);
                sequenceListener.onSequenceCanceled(null);
                return;
            }

            if (targets.size() == 2) {
                new TapTargetSequence(this).targets(targets).listener(sequenceListener).continueOnCancel(false).start();
            } else {
                showFreeVersionLimitationsDialog(() -> {
                    SharedPreferences.Editor editor = tourPrefs.edit();
                    editor.putBoolean(KEY_MAIN_INTERACTIVE_TOUR_SHOWN, true);
                    editor.apply();
                });
            }
        }
    }

    interface OnLimitationsDialogDismissed {
        void onDismissed();
    }

    private void showFreeVersionLimitationsDialog(OnLimitationsDialogDismissed onDismissedListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainHandwashing.this);
        builder.setTitle(getString(R.string.dialog_limitations_title));
        builder.setMessage(HtmlCompat.fromHtml(getString(R.string.dialog_limitations_message), HtmlCompat.FROM_HTML_MODE_LEGACY));
        builder.setPositiveButton(getString(R.string.dialog_ok), (dialog, which) -> {
            dialog.dismiss();
            if (onDismissedListener != null) {
                onDismissedListener.onDismissed();
            }
        });
        builder.setCancelable(false);
        if (!isFinishing() && !isDestroyed()) {
            builder.create().show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLeaderboardDataAsync();
        loadCustomLogoAsync(); // Use the new asynchronous method
        interactiveTourAttemptedThisSession = false;
        hasWindowFocusForTour = false;
    }

    private void setupgui() {
        img_custom_logo = findViewById(R.id.img_custom_logo);
        txt_datetime = findViewById(R.id.txt_datetime);
        edit_employee_number = findViewById(R.id.edit_employee_number);
        btn_start = findViewById(R.id.btn_start);
        table_top_handwashers = findViewById(R.id.table_top_handwashers);
        mainToolbar = findViewById(R.id.main_toolbar);
    }

    private void loadCustomLogoAsync() {
        backgroundExecutor.execute(() -> {
            SharedPreferences prefs = getSharedPreferences(AdminDashboardActivity.PREFS_NAME, MODE_PRIVATE);
            String logoPath = prefs.getString(AdminDashboardActivity.KEY_CUSTOM_LOGO_PATH, null);

            final Bitmap logoBitmap;
            if (logoPath != null) {
                File logoFile = new File(logoPath);
                if (logoFile.exists()) {
                    // This is the slow part that is now in the background
                    logoBitmap = BitmapFactory.decodeFile(logoFile.getAbsolutePath());
                } else {
                    logoBitmap = null;
                }
            } else {
                logoBitmap = null;
            }

            // Update the UI on the main thread
            runOnUiThread(() -> {
                if (img_custom_logo != null) {
                    if (logoBitmap != null) {
                        img_custom_logo.setImageBitmap(logoBitmap);
                        img_custom_logo.setVisibility(View.VISIBLE);
                    } else {
                        img_custom_logo.setVisibility(View.GONE);
                    }
                }
            });
        });
    }

    private void setupListeners() {
        if (btn_start == null || edit_employee_number == null) return;

        btn_start.setOnClickListener(v -> {
            String employeeNumberStr = edit_employee_number.getText().toString().trim();
            edit_employee_number.setError(null);

            if (employeeNumberStr.isEmpty()) {
                edit_employee_number.setError("Please enter employee number");
                edit_employee_number.requestFocus();
                return;
            }

            backgroundExecutor.execute(() -> {
                boolean employeeExists = dbHelper.doesEmployeeExist(employeeNumberStr);
                runOnUiThread(() -> {
                    if (employeeExists) {
                        Intent intent = new Intent(MainHandwashing.this, WetHandsActivity.class);
                        intent.putExtra("employee_number", employeeNumberStr);
                        intent.putExtra("overall_time_remaining", WetHandsActivity.TOTAL_PROCESS_DURATION_MS);
                        startActivity(intent);
                        edit_employee_number.setText("");
                    } else {
                        edit_employee_number.setError("Employee number not found");
                        edit_employee_number.requestFocus();
                    }
                });
            });
        });

        edit_employee_number.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                if (btn_start != null) {
                    btn_start.performClick();
                }
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null && getCurrentFocus() != null) {
                    imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                }
                return true;
            }
            return false;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_admin_login) {
            Intent intent = new Intent(MainHandwashing.this, AdminLoginActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadLeaderboardDataAsync() {
        if (table_top_handwashers == null || dbHelper == null) return;

        table_top_handwashers.removeAllViews();
        TableRow loadingRow = new TableRow(this);
        TextView loadingMsg = new TextView(this);
        loadingMsg.setText("Loading leaderboard...");
        loadingMsg.setGravity(Gravity.CENTER);
        TableRow.LayoutParams loadingParams = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
        loadingParams.span = 3;
        loadingRow.addView(loadingMsg, loadingParams);
        table_top_handwashers.addView(loadingRow);

        backgroundExecutor.execute(() -> {
            List<LeaderboardEntry> leaderboardData = dbHelper.getTopHandwashers();
            runOnUiThread(() -> populateLeaderboardTable(leaderboardData));
        });
    }

    private void populateLeaderboardTable(List<LeaderboardEntry> leaderboardData) {
        if (table_top_handwashers == null) return;

        table_top_handwashers.removeAllViews();
        TableRow headerRow = new TableRow(this);
        headerRow.setPadding(dpToPx(8), dpToPx(12), dpToPx(8), dpToPx(12));

        TextView rankHeader = createTableHeaderTextView("Rank");
        TextView nameHeader = createTableHeaderTextView("Name");
        TextView countHeader = createTableHeaderTextView("Washes");

        rankHeader.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.5f));
        nameHeader.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1.5f));
        countHeader.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));

        headerRow.addView(rankHeader);
        headerRow.addView(nameHeader);
        headerRow.addView(countHeader);
        table_top_handwashers.addView(headerRow);

        if (leaderboardData.isEmpty()) {
            TableRow emptyRow = new TableRow(this);
            TextView emptyMsg = new TextView(this);
            emptyMsg.setText("No handwashes recorded yet today!");
            emptyMsg.setTextSize(16);
            emptyMsg.setPadding(dpToPx(8), dpToPx(16), dpToPx(8), dpToPx(16));
            emptyMsg.setGravity(Gravity.CENTER);
            TableRow.LayoutParams emptyParams = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
            emptyParams.span = 3;
            emptyRow.addView(emptyMsg, emptyParams);
            table_top_handwashers.addView(emptyRow);
        } else {
            int rank = 1;
            for (LeaderboardEntry entry : leaderboardData) {
                TableRow dataRow = new TableRow(this);
                dataRow.setPadding(dpToPx(8), dpToPx(10), dpToPx(8), dpToPx(10));

                TextView rankView = createDataTextView(String.valueOf(rank) + ".", 20, Typeface.BOLD);
                rankView.setGravity(Gravity.CENTER);

                LinearLayout nameCellLayout = new LinearLayout(this);
                nameCellLayout.setOrientation(LinearLayout.HORIZONTAL);
                nameCellLayout.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
                ImageView starIcon = new ImageView(this);
                starIcon.setImageResource(R.drawable.ic_star_leaderboard);
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(20), dpToPx(20));
                iconParams.setMarginEnd(dpToPx(4));
                starIcon.setLayoutParams(iconParams);

                String firstName = entry.employeeName;
                String lastName = entry.lastName;
                String displayName = (firstName == null) ? "" : firstName;
                if (lastName != null && !lastName.isEmpty()) {
                    displayName = displayName.trim() + " " + lastName.charAt(0) + ".";
                } else if (firstName != null && firstName.equals("Guest") && (lastName == null || lastName.isEmpty())) {
                    displayName = "Guest";
                }
                displayName = displayName.trim();

                TextView nameView = createDataTextView(displayName, 20, Typeface.NORMAL);
                nameView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
                nameCellLayout.addView(starIcon);
                nameCellLayout.addView(nameView);

                TextView countView = createDataTextView(String.valueOf(entry.handwashCount), 20, Typeface.BOLD);
                countView.setGravity(Gravity.CENTER);

                dataRow.addView(rankView, new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.5f));
                dataRow.addView(nameCellLayout, new TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT, 1.5f));
                dataRow.addView(countView, new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
                table_top_handwashers.addView(dataRow);
                rank++;
            }
        }
    }

    private TextView createTableHeaderTextView(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            textView.setTextColor(ContextCompat.getColor(this, R.color.purple_200));
        } else {
            textView.setTextColor(ContextCompat.getColor(this, R.color.purple_500));
        }
        textView.setTextSize(16);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        return textView;
    }

    private TextView createDataTextView(String text, int textSize, int textStyle) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(textSize);
        textView.setTypeface(null, textStyle);
        textView.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        return textView;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timeUpdateHandler != null && updateTimeRunnable != null) {
            timeUpdateHandler.removeCallbacks(updateTimeRunnable);
        }
        backgroundExecutor.shutdown();
    }
}