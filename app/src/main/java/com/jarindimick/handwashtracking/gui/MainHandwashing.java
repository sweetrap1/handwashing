package com.jarindimick.handwashtracking.gui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu; // Import Menu
import android.view.MenuInflater; // Import MenuInflater
import android.view.MenuItem; // Import MenuItem
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

public class MainHandwashing extends AppCompatActivity {

    private ImageView img_logo;
    private TextView txt_datetime;
    private EditText edit_employee_number;
    private Button btn_start;
    private TableLayout table_top_handwashers;
    private Handler handler = new Handler();
    private Runnable updateTimeRunnable;
    private DatabaseHelper dbHelper;
    private List<com.jarindimick.handwashtracking.gui.LeaderboardEntry> leaderboardData = new ArrayList<>();
    // Removed btn_admin_login declaration

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
        dbHelper = new DatabaseHelper(this);
        populateLeaderboardTable();
    }

    private void setupgui() {
        img_logo = findViewById(R.id.img_logo);
        txt_datetime = findViewById(R.id.txt_datetime);
        edit_employee_number = findViewById(R.id.edit_employee_number);
        btn_start = findViewById(R.id.btn_start);
        table_top_handwashers = findViewById(R.id.table_top_handwashers);
        // Removed btn_admin_login initialization

        //Hide action bar - you can remove this if you want the action bar for the menu icon
        // getSupportActionBar().hide();
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
                handler.postDelayed(this, 1000);
            }
        };
        handler.postDelayed(updateTimeRunnable, 0);
    }

    private void setupListeners() {
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String employeeNumber = edit_employee_number.getText().toString();
                if (!employeeNumber.isEmpty()) {
                    saveEmployeeData(employeeNumber);
                } else {
                    Toast.makeText(MainHandwashing.this, "Please enter employee number", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Removed btn_admin_login listener
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu); // Inflate your menu resource
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        if (item.getItemId() == R.id.menu_admin_login) {
            Intent intent = new Intent(MainHandwashing.this, AdminLoginActivity.class);
            startActivity(intent);
            return true;
        }
        // Handle other menu item clicks here (e.g., settings)
        return super.onOptionsItemSelected(item);
    }


    public class LeaderboardEntry {
        String employeeNumber;
        String employeeName;
        int handwashCount;

        public LeaderboardEntry(String employeeNumber, String employeeName, int handwashCount) {
            this.employeeNumber = employeeNumber;
            this.employeeName = employeeName;
            this.handwashCount = handwashCount;
        }
    }

    private void saveEmployeeData(String employeeNumber) {
        // Log the handwash
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault());
        String washDate = now.format(dateFormatter);
        String washTime = now.format(timeFormatter);

        long logResult = dbHelper.insertHandwashLog(employeeNumber, washDate, washTime, ""); //  Empty photoPath for now

        if (logResult != -1) {
            // Optionally, you might want to update employee data here if needed
            // Since we are now allowing manual employee addition,
            // we should still ensure the employee exists, but we don't need
            // to call insertEmployee with only the number here if it's handled
            // in the admin dashboard.
            // However, keeping it here ensures that even if an employee isn't
            // manually added in the admin panel, a handwash log will create
            // a basic employee entry. You might adjust this logic based on
            // whether all employees MUST be added via the admin panel.
            long empResult = dbHelper.insertEmployee(employeeNumber); // This will now update or insert a basic entry
            if (empResult != -1) {
                Toast.makeText(this, "Handwash recorded", Toast.LENGTH_SHORT).show();
                edit_employee_number.setText("");
                populateLeaderboardTable(); // Refresh the leaderboard
            } else {
                Toast.makeText(this, "Handwash recorded, but error updating employee data", Toast.LENGTH_SHORT).show();
            }


        } else {
            Toast.makeText(this, "Error recording handwash", Toast.LENGTH_SHORT).show();
        }
    }

    private void populateLeaderboardTable() {
        table_top_handwashers.removeAllViews();

        // Add header row
        TableRow headerRow = new TableRow(this);
        headerRow.setBackgroundColor(getColor(R.color.teal_700));
        headerRow.setPadding(8, 8, 8, 8);

        TextView nameHeader = createTableHeaderTextView("Emp #");
        TextView countHeader = createTableHeaderTextView("Handwashes");

        headerRow.addView(nameHeader);
        headerRow.addView(countHeader);
        table_top_handwashers.addView(headerRow);

        // Get data from the database
        leaderboardData = dbHelper.getTopHandwashers();

        // Add data rows
        for (com.jarindimick.handwashtracking.gui.LeaderboardEntry entry : leaderboardData) {
            TableRow row = new TableRow(this);
            row.setPadding(8, 8, 8, 8);

            TextView nameView = createDataTextView(entry.employeeNumber);
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
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}