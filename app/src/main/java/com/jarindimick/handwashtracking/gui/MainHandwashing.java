package com.jarindimick.handwashtracking.gui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
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
import androidx.appcompat.app.AlertDialog; // Keep for other dialogs if any, or if you add one here
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.text.HtmlCompat; // For the limitations dialog, if you re-add it
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

public class MainHandwashing extends AppCompatActivity {

    private ImageView img_custom_logo;
    private TextView txt_datetime;
    private EditText edit_employee_number;
    private Button btn_start;
    private TableLayout table_top_handwashers;
    private Handler timeUpdateHandler = new Handler(Looper.getMainLooper());
    private Runnable updateTimeRunnable;
    private DatabaseHelper dbHelper;
    private List<LeaderboardEntry> leaderboardData = new ArrayList<>();
    private Toolbar mainToolbar;

    // SharedPreferences keys for the interactive tour
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

        setupgui(); // Initializes mainToolbar among other views

        if (mainToolbar != null) { // Check if mainToolbar was found
            setSupportActionBar(mainToolbar);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        loadCustomLogo();
        startUpdatingTime();
        setupListeners();
        dbHelper = new DatabaseHelper(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && !interactiveTourAttemptedThisSession) {
            hasWindowFocusForTour = true;
            // Post a small delay to give the UI (especially menu items) time to fully render
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (hasWindowFocusForTour) { // Re-check focus in case it was lost during the delay
                    showInteractiveTourIfNeeded();
                }
            }, 300); // 300ms delay, adjust if necessary
        } else if (!hasFocus) {
            hasWindowFocusForTour = false;
        }
    }

    private void showInteractiveTourIfNeeded() {
        final SharedPreferences tourPrefs = getSharedPreferences(PREFS_MAIN_TOUR_FILE, MODE_PRIVATE);
        boolean tourShown = tourPrefs.getBoolean(KEY_MAIN_INTERACTIVE_TOUR_SHOWN, false);

        if (tourShown) { // If permanently shown, do nothing.
            return;
        }

        // If not permanently shown, but we already tried (and possibly failed) this session, don't try again.
        if (interactiveTourAttemptedThisSession){
            Log.d("MainHandwashing", "Interactive tour already attempted this session.");
            return;
        }

        interactiveTourAttemptedThisSession = true; // Mark as attempted for this current activity instance

        // Ensure views are ready and the menu item exists
        if (edit_employee_number != null && mainToolbar != null && mainToolbar.getMenu() != null && mainToolbar.getMenu().size() > 0 && mainToolbar.getMenu().findItem(R.id.menu_admin_login) != null) {

            // --- Customizable Wording for Admin Access ---
            String adminTitle = "Admin Features"; // YOUR NEW TITLE HERE
            String adminDescription = "Access employee management & app settings. Default login: admin/admin (please change!)."; // YOUR NEW DESCRIPTION HERE
            // --- End of Customizable Wording ---

            TapTargetSequence.Listener sequenceListener = new TapTargetSequence.Listener() {
                @Override
                public void onSequenceFinish() {
                    Log.d("MainHandwashing", "Interactive tour sequence finished.");
                    showFreeVersionLimitationsDialog(() -> { // Show limitations dialog
                        SharedPreferences.Editor editor = tourPrefs.edit();
                        editor.putBoolean(KEY_MAIN_INTERACTIVE_TOUR_SHOWN, true);
                        editor.apply();
                        Log.d("MainHandwashing", "Limitations dialog dismissed, tour preference saved.");
                    });
                }

                @Override
                public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {
                    // Called when a step is completed
                }

                @Override
                public void onSequenceCanceled(TapTarget lastTarget) {
                    Log.w("MainHandwashing", "Interactive tour sequence cancelled/failed.");
                    // If cancelled (e.g., target not found by library), still show limitations and mark as done
                    showFreeVersionLimitationsDialog(() -> {
                        SharedPreferences.Editor editor = tourPrefs.edit();
                        editor.putBoolean(KEY_MAIN_INTERACTIVE_TOUR_SHOWN, true);
                        editor.apply();
                        Log.d("MainHandwashing", "Limitations dialog dismissed after tour cancel, preference saved.");
                    });
                }
            };

            ArrayList<TapTarget> targets = new ArrayList<>();

            targets.add(TapTarget.forView(edit_employee_number,
                            "Guest User Option",
                            "Enter employee number '0' to log a handwash if you are not registered.")
                    .outerCircleColor(R.color.purple_500)      // Example color
                    .targetCircleColor(android.R.color.white)
                    .titleTextColor(android.R.color.white)
                    .descriptionTextColor(android.R.color.white)
                    .textTypeface(Typeface.SANS_SERIF)
                    .dimColor(android.R.color.black)
                    .drawShadow(true)
                    .cancelable(false)
                    .tintTarget(true)
                    .transparentTarget(false)
                    .id(1)
                    .targetRadius(70));

            try {
                targets.add(TapTarget.forToolbarOverflow(mainToolbar, // Targets the overflow menu icon
                                adminTitle,
                                adminDescription)
                        .outerCircleColor(R.color.purple_500)
                        .targetCircleColor(android.R.color.white)
                        .titleTextColor(android.R.color.white)
                        .descriptionTextColor(android.R.color.white)
                        .textTypeface(Typeface.SANS_SERIF)
                        .dimColor(android.R.color.black)
                        .drawShadow(true)
                        .cancelable(false)
                        .id(2));
            } catch (Exception e) { // Catch if forToolbarOverflow fails (e.g. no overflow visible)
                Log.e("TapTarget", "Failed to create TapTarget for toolbar overflow. Menu might not be ready or no overflow items.", e);
                sequenceListener.onSequenceCanceled(null); // Trigger cancel to show limitations and set pref
                return; // Don't try to start an empty or incomplete sequence
            }


            if (targets.size() == 2) { // Ensure both targets were added successfully
                new TapTargetSequence(this)
                        .targets(targets)
                        .listener(sequenceListener)
                        .continueOnCancel(false)
                        .start();
                Log.d("MainHandwashing", "Interactive tour sequence started with " + targets.size() + " targets.");
            } else {
                Log.w("MainHandwashing", "Not all targets for interactive tour were created. Tour skipped. Targets count: " + targets.size());
                // Mark tour as shown to prevent retrying if a target is persistently missing
                showFreeVersionLimitationsDialog(() -> {
                    SharedPreferences.Editor editor = tourPrefs.edit();
                    editor.putBoolean(KEY_MAIN_INTERACTIVE_TOUR_SHOWN, true);
                    editor.apply();
                });
            }

        } else {
            Log.w("MainHandwashing", "Interactive tour prerequisites not met (views null, menu not ready, or already attempted this session).");
            // If views are null, it means setupgui might not have run or IDs are wrong.
            // If it was already attempted this session, that's fine.
            // If tourShown is true, it's also fine.
            // Only reset interactiveTourAttemptedThisSession if you want it to retry on next focus if views were the issue.
            // For a one-time tour, if it fails to start due to views, the SharedPreferences flag won't be set,
            // so it will try again next app launch.
        }
    }

    // Define the OnLimitationsDialogDismissed interface
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

        AlertDialog dialog = builder.create();
        if (!isFinishing() && !isDestroyed()) { // Ensure activity is active before showing dialog
            dialog.show();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        populateLeaderboardTable();
        loadCustomLogo();
        interactiveTourAttemptedThisSession = false; // Reset session flag on resume
        hasWindowFocusForTour = false;               // Reset focus flag
    }

    private void setupgui() {
        img_custom_logo = findViewById(R.id.img_custom_logo);
        txt_datetime = findViewById(R.id.txt_datetime);
        edit_employee_number = findViewById(R.id.edit_employee_number);
        btn_start = findViewById(R.id.btn_start);
        table_top_handwashers = findViewById(R.id.table_top_handwashers);
        mainToolbar = findViewById(R.id.main_toolbar); // Ensure this is initialized
    }

    private void loadCustomLogo() {
        SharedPreferences prefs = getSharedPreferences(AdminDashboardActivity.PREFS_NAME, MODE_PRIVATE);
        String logoPath = prefs.getString(AdminDashboardActivity.KEY_CUSTOM_LOGO_PATH, null);
        if (img_custom_logo == null) return; // Guard against null view

        if (logoPath != null) {
            File logoFile = new File(logoPath);
            if (logoFile.exists()) {
                try {
                    img_custom_logo.setImageBitmap(BitmapFactory.decodeFile(logoFile.getAbsolutePath()));
                    img_custom_logo.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    Log.e("MainHandwashing", "Error loading custom logo: " + e.getMessage());
                    img_custom_logo.setVisibility(View.GONE);
                }
            } else {
                img_custom_logo.setVisibility(View.GONE);
            }
        } else {
            img_custom_logo.setVisibility(View.GONE);
        }
    }

    private void updateDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, uuuu", Locale.getDefault());
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault());
        if (txt_datetime != null) { // Guard against null view
            txt_datetime.setText(String.format("%s, %s", now.format(dateFormatter), now.format(timeFormatter)));
        }
    }

    private void startUpdatingTime() {
        updateTimeRunnable = new Runnable() {
            @Override
            public void run() {
                updateDateTime();
                timeUpdateHandler.postDelayed(this, 1000);
            }
        };
        timeUpdateHandler.postDelayed(updateTimeRunnable, 0);
    }

    private void setupListeners() {
        if (btn_start == null || edit_employee_number == null) return; // Guard

        btn_start.setOnClickListener(v -> {
            String employeeNumberStr = edit_employee_number.getText().toString().trim();
            edit_employee_number.setError(null);

            if (employeeNumberStr.isEmpty()) {
                edit_employee_number.setError("Please enter employee number");
                edit_employee_number.requestFocus();
                return;
            }

            if (dbHelper.doesEmployeeExist(employeeNumberStr)) {
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

        edit_employee_number.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
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

    private void populateLeaderboardTable() {
        if (table_top_handwashers == null || dbHelper == null) return; // Guard

        table_top_handwashers.removeAllViews();
        TableRow headerRow = new TableRow(this);
        TableRow.LayoutParams headerParams = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
        headerRow.setLayoutParams(headerParams);
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
        leaderboardData = dbHelper.getTopHandwashers();

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
                int dataTextSize = 20;
                TextView rankView = createDataTextView(String.valueOf(rank) + ".", dataTextSize, Typeface.BOLD);
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
                Log.d("LeaderboardDebug", "Rank: " + rank + ", Employee: " + entry.employeeNumber + ", DisplayName: '" + displayName + "'");
                TextView nameView = createDataTextView(displayName, dataTextSize, Typeface.NORMAL);
                LinearLayout.LayoutParams nameViewParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                nameView.setLayoutParams(nameViewParams);
                nameView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
                nameCellLayout.addView(starIcon);
                nameCellLayout.addView(nameView);
                TextView countView = createDataTextView(String.valueOf(entry.handwashCount), dataTextSize, Typeface.BOLD);
                countView.setGravity(Gravity.CENTER);
                TableRow.LayoutParams rankCellParams = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.5f);
                TableRow.LayoutParams nameCellParams = new TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT, 1.5f);
                TableRow.LayoutParams countCellParams = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f);
                dataRow.addView(rankView, rankCellParams);
                dataRow.addView(nameCellLayout, nameCellParams);
                dataRow.addView(countView, countCellParams);
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
        timeUpdateHandler.removeCallbacks(updateTimeRunnable);
    }
}