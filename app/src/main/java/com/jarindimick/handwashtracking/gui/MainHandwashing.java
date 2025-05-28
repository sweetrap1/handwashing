package com.jarindimick.handwashtracking.gui;

import android.content.Intent;
import android.content.res.Configuration; // Import for night mode check
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
// import android.util.TypedValue; // Not needed if getColorFromAttr is removed
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
// import androidx.annotation.AttrRes; // Not needed
// import androidx.annotation.ColorInt;  // Not needed
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.jarindimick.handwashtracking.R;
import com.jarindimick.handwashtracking.databasehelper.DatabaseHelper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainHandwashing extends AppCompatActivity {

    private ImageView img_logo;
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
        startUpdatingTime();
        setupListeners();
        dbHelper = new DatabaseHelper(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        populateLeaderboardTable();
    }

    private void setupgui() {
        txt_datetime = findViewById(R.id.txt_datetime);
        edit_employee_number = findViewById(R.id.edit_employee_number);
        btn_start = findViewById(R.id.btn_start);
        table_top_handwashers = findViewById(R.id.table_top_handwashers);
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
                if (employeeNumberStr.isEmpty()) {
                    Toast.makeText(MainHandwashing.this, "Please enter employee number", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (dbHelper.doesEmployeeExist(employeeNumberStr)) {
                    Intent intent = new Intent(MainHandwashing.this, WetHandsActivity.class);
                    intent.putExtra("employee_number", employeeNumberStr);
                    intent.putExtra("overall_time_remaining", WetHandsActivity.TOTAL_PROCESS_DURATION_MS);
                    startActivity(intent);
                    edit_employee_number.setText("");
                } else {
                    Toast.makeText(MainHandwashing.this, "Employee number not found", Toast.LENGTH_SHORT).show();
                }
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_admin_login) {
            Intent intent = new Intent(MainHandwashing.this, AdminLoginActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void populateLeaderboardTable() {
        table_top_handwashers.removeAllViews();

        TableRow headerRow = new TableRow(this);
        TableRow.LayoutParams headerParams = new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT);
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
            TableRow.LayoutParams emptyParams = new TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT);
            emptyParams.span = 3;
            emptyRow.addView(emptyMsg, emptyParams);
            table_top_handwashers.addView(emptyRow);
        } else {
            int rank = 1;
            for (LeaderboardEntry entry : leaderboardData) {
                TableRow dataRow = new TableRow(this);
                dataRow.setPadding(dpToPx(8), dpToPx(10), dpToPx(8), dpToPx(10));

                TextView rankView = createDataTextView(String.valueOf(rank) + ".", 18, Typeface.BOLD);
                rankView.setGravity(Gravity.CENTER);

                LinearLayout nameCellLayout = new LinearLayout(this);
                nameCellLayout.setOrientation(LinearLayout.HORIZONTAL);
                nameCellLayout.setGravity(Gravity.CENTER);


                ImageView starIcon = new ImageView(this);
                starIcon.setImageResource(R.drawable.ic_star_leaderboard);
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(20), dpToPx(20));
                iconParams.setMarginEnd(dpToPx(8));
                starIcon.setLayoutParams(iconParams);

                TextView nameView = createDataTextView(entry.employeeName, 18, Typeface.NORMAL);
                nameView.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);


                nameCellLayout.addView(starIcon);
                nameCellLayout.addView(nameView);

                TextView countView = createDataTextView(String.valueOf(entry.handwashCount), 18, Typeface.BOLD);
                countView.setGravity(Gravity.CENTER);

                dataRow.addView(rankView, new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.5f));
                dataRow.addView(nameCellLayout, new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1.5f));
                dataRow.addView(countView, new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));

                table_top_handwashers.addView(dataRow);
                rank++;
            }
        }
    }

    // Removed getColorFromAttr method as R.attr.colorPrimary was not resolving

    private TextView createTableHeaderTextView(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);

        // Directly check for night mode and set color
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            textView.setTextColor(ContextCompat.getColor(this, R.color.purple_200)); // Light purple for dark theme
        } else {
            textView.setTextColor(ContextCompat.getColor(this, R.color.purple_500)); // Medium purple for light theme
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
        // Consider setting a theme-aware text color for data rows if needed, e.g.,
        // textView.setTextColor(ContextCompat.getColor(this, android.R.color.primary_text_light));
        // or using a theme attribute like ?android:attr/textColorPrimary
        return textView;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateTimeRunnable);
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}
