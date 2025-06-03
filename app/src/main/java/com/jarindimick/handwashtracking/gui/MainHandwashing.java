package com.jarindimick.handwashtracking.gui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull; // Added for @NonNull in onOptionsItemSelected
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.jarindimick.handwashtracking.R;
import com.jarindimick.handwashtracking.databasehelper.DatabaseHelper;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.view.inputmethod.EditorInfo;
import android.view.KeyEvent;
import android.widget.TextView; // For TextView.OnEditorActionListener
import android.view.inputmethod.InputMethodManager;
import android.content.Context;

public class MainHandwashing extends AppCompatActivity {

    private ImageView img_custom_logo;
    private TextView txt_datetime;
    private EditText edit_employee_number;
    private Button btn_start;
    private TableLayout table_top_handwashers;
    private Handler handler = new Handler();
    private Runnable updateTimeRunnable;
    private DatabaseHelper dbHelper;
    private List<LeaderboardEntry> leaderboardData = new ArrayList<>();
    private Toolbar mainToolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_handwashing);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mainToolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(mainToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupgui();
        loadCustomLogo();
        startUpdatingTime();
        setupListeners();
        dbHelper = new DatabaseHelper(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        populateLeaderboardTable();
        loadCustomLogo();
    }

    private void setupgui() {
        img_custom_logo = findViewById(R.id.img_custom_logo);
        txt_datetime = findViewById(R.id.txt_datetime);
        edit_employee_number = findViewById(R.id.edit_employee_number);
        btn_start = findViewById(R.id.btn_start);
        table_top_handwashers = findViewById(R.id.table_top_handwashers);
    }

    private void loadCustomLogo() {
        SharedPreferences prefs = getSharedPreferences(AdminDashboardActivity.PREFS_NAME, MODE_PRIVATE);
        String logoPath = prefs.getString(AdminDashboardActivity.KEY_CUSTOM_LOGO_PATH, null);

        if (logoPath != null) {
            File logoFile = new File(logoPath);
            if (logoFile.exists()) {
                try {
                    android.graphics.Bitmap bitmap = BitmapFactory.decodeFile(logoFile.getAbsolutePath());
                    img_custom_logo.setImageBitmap(bitmap);
                    img_custom_logo.setVisibility(View.VISIBLE);
                    Log.d("MainHandwashing", "Custom logo loaded from: " + logoPath);
                } catch (Exception e) {
                    Log.e("MainHandwashing", "Error loading custom logo bitmap: " + e.getMessage());
                    img_custom_logo.setVisibility(View.GONE);
                }
            } else {
                Log.w("MainHandwashing", "Custom logo path exists in prefs, but file not found: " + logoPath);
                img_custom_logo.setVisibility(View.GONE);
            }
        } else {
            Log.d("MainHandwashing", "No custom logo path found in prefs.");
            img_custom_logo.setVisibility(View.GONE);
        }
    }

    private void updateDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, uuuu", Locale.getDefault());
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault());
        String formattedDate = now.format(dateFormatter);
        String formattedTime = now.format(timeFormatter);
        txt_datetime.setText(String.format("%s, %s", formattedDate, formattedTime));
    }

    private void startUpdatingTime() {
        updateTimeRunnable = new Runnable() {
            @Override
            public void run() {
                updateDateTime();
                handler.postDelayed(this, 1000);
            }
        };
        handler.postDelayed(updateTimeRunnable, 0);
    }

    private void setupListeners() {
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String employeeNumberStr = edit_employee_number.getText().toString().trim();

                // Clear previous errors
                edit_employee_number.setError(null);

                if (employeeNumberStr.isEmpty()) {
                    // Toast.makeText(MainHandwashing.this, "Please enter employee number", Toast.LENGTH_SHORT).show(); // OLD
                    edit_employee_number.setError("Please enter employee number"); // NEW
                    edit_employee_number.requestFocus(); // Optional: bring focus to the field
                    return;
                }

                if (dbHelper.doesEmployeeExist(employeeNumberStr)) { //
                    Intent intent = new Intent(MainHandwashing.this, WetHandsActivity.class); //
                    intent.putExtra("employee_number", employeeNumberStr); //
                    intent.putExtra("overall_time_remaining", WetHandsActivity.TOTAL_PROCESS_DURATION_MS); //
                    startActivity(intent); //
                    edit_employee_number.setText("");
                } else {
                    // Toast.makeText(MainHandwashing.this, "Employee number not found", Toast.LENGTH_SHORT).show(); // OLD
                    edit_employee_number.setError("Employee number not found"); // NEW
                    edit_employee_number.requestFocus(); // Optional
                }
            }
        });

        edit_employee_number.setOnEditorActionListener(new TextView.OnEditorActionListener() { //
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                        (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    if (btn_start != null) {
                        btn_start.performClick();
                    }
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE); //
                    if (imm != null && getCurrentFocus() != null) {
                        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                    }
                    return true;
                }
                return false;
            }
        });
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) { // Added @NonNull
        if (item.getItemId() == R.id.menu_admin_login) {
            Intent intent = new Intent(MainHandwashing.this, AdminLoginActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    // ... other methods ...

    private void populateLeaderboardTable() {
        table_top_handwashers.removeAllViews(); //

        TableRow headerRow = new TableRow(this); //
        TableRow.LayoutParams headerParams = new TableRow.LayoutParams( //
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT);
        headerRow.setLayoutParams(headerParams); //
        headerRow.setPadding(dpToPx(8), dpToPx(12), dpToPx(8), dpToPx(12)); //

        TextView rankHeader = createTableHeaderTextView("Rank"); //
        TextView nameHeader = createTableHeaderTextView("Name");   //
        TextView countHeader = createTableHeaderTextView("Washes"); //

        rankHeader.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.5f)); //
        nameHeader.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1.5f)); //
        countHeader.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)); //

        headerRow.addView(rankHeader); //
        headerRow.addView(nameHeader); //
        headerRow.addView(countHeader); //
        table_top_handwashers.addView(headerRow); //

        leaderboardData = dbHelper.getTopHandwashers(); //

        if (leaderboardData.isEmpty()) { //
            TableRow emptyRow = new TableRow(this); //
            TextView emptyMsg = new TextView(this); //
            emptyMsg.setText("No handwashes recorded yet today!"); //
            emptyMsg.setTextSize(16); //
            emptyMsg.setPadding(dpToPx(8), dpToPx(16), dpToPx(8), dpToPx(16)); //
            emptyMsg.setGravity(Gravity.CENTER); //
            TableRow.LayoutParams emptyParams = new TableRow.LayoutParams( //
                    TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT);
            emptyParams.span = 3; //
            emptyRow.addView(emptyMsg, emptyParams); //
            table_top_handwashers.addView(emptyRow); //
        } else {
            int rank = 1;
            for (LeaderboardEntry entry : leaderboardData) { //
                TableRow dataRow = new TableRow(this); //
                dataRow.setPadding(dpToPx(8), dpToPx(10), dpToPx(8), dpToPx(10)); //

                int dataTextSize = 20; //

                TextView rankView = createDataTextView(String.valueOf(rank) + ".", dataTextSize, Typeface.BOLD); //
                rankView.setGravity(Gravity.CENTER); //

                LinearLayout nameCellLayout = new LinearLayout(this); //
                nameCellLayout.setOrientation(LinearLayout.HORIZONTAL); //
                // THIS IS THE KEY CHANGE:
                nameCellLayout.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL); //

                ImageView starIcon = new ImageView(this); //
                starIcon.setImageResource(R.drawable.ic_star_leaderboard); //
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams( //
                        dpToPx(20),
                        dpToPx(20)
                );
                iconParams.setMarginEnd(dpToPx(4)); //
                starIcon.setLayoutParams(iconParams); //

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

                TextView nameView = createDataTextView(displayName, dataTextSize, Typeface.NORMAL); //
                LinearLayout.LayoutParams nameViewParams = new LinearLayout.LayoutParams( //
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1.0f
                );
                nameView.setLayoutParams(nameViewParams); //
                nameView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL); // Text within the TextView is still START aligned

                nameCellLayout.addView(starIcon); //
                nameCellLayout.addView(nameView); //

                TextView countView = createDataTextView(String.valueOf(entry.handwashCount), dataTextSize, Typeface.BOLD); //
                countView.setGravity(Gravity.CENTER); //

                TableRow.LayoutParams rankCellParams = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.5f); //
                TableRow.LayoutParams nameCellParams = new TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT, 1.5f); //
                TableRow.LayoutParams countCellParams = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f); //

                dataRow.addView(rankView, rankCellParams); //
                dataRow.addView(nameCellLayout, nameCellParams); //
                dataRow.addView(countView, countCellParams); //

                table_top_handwashers.addView(dataRow); //
                rank++;
            }
        }
    }

    // ... rest of MainHandwashing.java ...
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
        // Let the specific addView call determine the gravity for the cell/content
        textView.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        return textView;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateTimeRunnable);
        // dbHelper.close(); // SQLiteOpenHelper handles closing.
    }
}