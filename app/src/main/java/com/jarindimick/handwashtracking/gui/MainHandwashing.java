package com.jarindimick.handwashtracking.gui;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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

// Define a simple data class to represent an employee's leaderboard entry
class LeaderboardEntry {
    String employeeNumber;
    String employeeName;
    int handwashCount;

    public LeaderboardEntry(String employeeNumber, String employeeName, int handwashCount) {
        this.employeeNumber = employeeNumber;
        this.employeeName = employeeName;
        this.handwashCount = handwashCount;
    }
}

public class MainHandwashing extends AppCompatActivity {
    private ImageView img_logo;
    private TextView txt_datetime;
    private TextView txt_title;
    private EditText edit_employee_number;
    private Button btn_start;
    private TextView txt_top_handwashers_title;
    private TableLayout table_top_handwashers;
    private Button btn_admin_login;
    private Handler handler = new Handler();
    private Runnable updateTimeRunnable;
    private DatabaseHelper dbHelper;;

    // Mock data for the leaderboard (replace with actual data later)
    private List<LeaderboardEntry> leaderboardData = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_handwashing);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        setupgui();
        startUpdatingTime();
        setupListeners();
        initializeMockLeaderboardData();  // Initialize mock data
        populateLeaderboardTable();       // Populate the table
    }

    private void setupgui() {
        img_logo = findViewById(R.id.img_logo);
        txt_datetime = findViewById(R.id.txt_datetime);
        txt_title = findViewById(R.id.txt_title);
        edit_employee_number = findViewById(R.id.edit_employee_number);
        btn_start = findViewById(R.id.btn_start);
        txt_top_handwashers_title = findViewById(R.id.txt_top_handwashers_title);
        table_top_handwashers = findViewById(R.id.table_top_handwashers);
        btn_admin_login = findViewById(R.id.btn_admin_login);
    }

    private void updateDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault());
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
                handler.postDelayed(this, 1000); // Update every 1 second
            }
        };
        handler.postDelayed(updateTimeRunnable, 0); // Start immediately
    }

    private void setupListeners() {
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String employeeNumber = edit_employee_number.getText().toString();
                if (!employeeNumber.isEmpty()) {
                    // For now, just display a toast.  Later, we'll handle the handwash recording.
                    Toast.makeText(MainHandwashing.this, "Start handwashing for employee: " + employeeNumber, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainHandwashing.this, "Please enter employee number", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btn_admin_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // For now, just display a toast.  Later, we'll navigate to the admin login.
                Toast.makeText(MainHandwashing.this, "Admin login clicked", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Initialize mock leaderboard data
    private void initializeMockLeaderboardData() {
        leaderboardData.add(new LeaderboardEntry("12345", "Alice Smith", 120));
        leaderboardData.add(new LeaderboardEntry("67890", "Bob Johnson", 95));
        leaderboardData.add(new LeaderboardEntry("24680", "Charlie Williams", 80));
        leaderboardData.add(new LeaderboardEntry("13579", "David Brown", 72));
        leaderboardData.add(new LeaderboardEntry("11223", "Emily Davis", 61));
    }

    // Populate the TableLayout with leaderboard data
    private void populateLeaderboardTable() {
        table_top_handwashers.removeAllViews();  // Clear existing rows (except header)

        // Add header row
        TableRow headerRow = new TableRow(this);
        headerRow.setBackgroundColor(getColor(R.color.teal_700)); // Example color
        headerRow.setPadding(8, 8, 8, 8);

        TextView nameHeader = createTableHeaderTextView("Name");
        TextView countHeader = createTableHeaderTextView("Handwashes");

        headerRow.addView(nameHeader);
        headerRow.addView(countHeader);
        table_top_handwashers.addView(headerRow);

        // Add data rows
        for (LeaderboardEntry entry : leaderboardData) {
            TableRow row = new TableRow(this);
            row.setPadding(8, 8, 8, 8);

            TextView nameView = createDataTextView(entry.employeeName);
            TextView countView = createDataTextView(String.valueOf(entry.handwashCount));

            row.addView(nameView);
            row.addView(countView);
            table_top_handwashers.addView(row);
        }
    }

    // Helper method to create TextView for table header
    private TextView createTableHeaderTextView(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextColor(getColor(R.color.white));
        textView.setTextSize(18);
        textView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        textView.setGravity(android.view.Gravity.CENTER);
        return textView;
    }

    // Helper method to create TextView for table data
    private TextView createDataTextView(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(16);
        textView.setGravity(android.view.Gravity.CENTER);
        return textView;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateTimeRunnable);
    }
}