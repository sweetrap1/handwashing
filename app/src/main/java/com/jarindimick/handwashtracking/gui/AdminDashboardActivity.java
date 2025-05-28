package com.jarindimick.handwashtracking.gui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.AssetManager;
// import android.database.Cursor; // Not directly used, queries are in DatabaseHelper
// import android.database.sqlite.SQLiteDatabase; // Not directly used
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge; // Import for EdgeToEdge
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;       // Import for Insets
import androidx.core.view.ViewCompat;     // Import for ViewCompat
import androidx.core.view.WindowInsetsCompat; // Import for WindowInsetsCompat
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jarindimick.handwashtracking.R;
import com.jarindimick.handwashtracking.databasehelper.DatabaseHelper;

// import org.mindrot.jbcrypt.BCrypt; // Not directly used here, handled in DatabaseHelper

import java.io.BufferedReader;
import java.io.File;
// import java.io.FileReader; // Not used for assets
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AdminDashboardActivity extends AppCompatActivity {
    private static final String TAG = "AdminDashboardActivity";

    // UI elements for Overview
    private TextView txt_overview_total_washes_today;
    private TextView txt_overview_active_employees;
    private TextView txt_overview_top_washer_today;

    // UI elements for Download Data
    private EditText edit_download_start_date;
    private EditText edit_download_end_date;
    private RadioGroup radio_download_type;
    private Button btn_download_data;

    // UI elements for Search Handwashes
    private EditText edit_search_first_name;
    private EditText edit_search_last_name;
    private EditText edit_search_employee_id;
    private EditText edit_search_start_date;
    private EditText edit_search_end_date;
    private Button btn_search_handwashes;
    private RecyclerView recycler_search_results;
    private HandwashLogAdapter handwashLogAdapter;

    private TextView txt_message;

    // UI elements for Other Buttons
    private Button btn_logout;
    private Button btn_delete_data;
    private Button btn_import_employees;

    // UI elements for changing admin password
    private EditText edit_old_password;
    private EditText edit_new_password;
    private EditText edit_confirm_new_password;
    private Button btn_change_password;

    // UI elements for adding new employee
    private EditText edit_add_employee_number;
    private EditText edit_add_first_name;
    private EditText edit_add_last_name;
    private EditText edit_add_department;
    private Button btn_add_employee;

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // Enable Edge-to-Edge display
        setContentView(R.layout.activity_admin_dashboard);

        // Apply window insets listener to the root content view
        // The root content view in activity_admin_dashboard.xml is the ConstraintLayout with id "main"
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Apply padding to the view 'v' (which is the ConstraintLayout with id 'main')
            // This ConstraintLayout already has android:padding="16dp".
            // The systemBars.top/bottom will be in addition to this if not handled carefully.
            // It's often better to set the root layout's padding to 0dp initially in XML,
            // and let this listener manage all padding.
            // For now, this will add to existing padding.
            // Let's adjust to set the padding directly, overriding XML for these edges.
            v.setPadding(systemBars.left + dpToPx(16), // Keep original horizontal padding
                    systemBars.top + dpToPx(16),    // Add original top padding
                    systemBars.right + dpToPx(16),  // Keep original horizontal padding
                    systemBars.bottom + dpToPx(16)); // Add original bottom padding
            return insets;
        });


        if (getSupportActionBar() != null) getSupportActionBar().hide();

        dbHelper = new DatabaseHelper(this);
        setupgui();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAdminOverviewData();
    }

    private void setupgui() {
        txt_overview_total_washes_today = findViewById(R.id.txt_overview_total_washes_today);
        txt_overview_active_employees = findViewById(R.id.txt_overview_active_employees);
        txt_overview_top_washer_today = findViewById(R.id.txt_overview_top_washer_today);

        edit_download_start_date = findViewById(R.id.edit_download_start_date);
        edit_download_end_date = findViewById(R.id.edit_download_end_date);
        radio_download_type = findViewById(R.id.radio_download_type);
        btn_download_data = findViewById(R.id.btn_download_data);

        edit_search_first_name = findViewById(R.id.edit_search_first_name);
        edit_search_last_name = findViewById(R.id.edit_search_last_name);
        edit_search_employee_id = findViewById(R.id.edit_search_employee_id);
        edit_search_start_date = findViewById(R.id.edit_search_start_date);
        edit_search_end_date = findViewById(R.id.edit_search_end_date);
        btn_search_handwashes = findViewById(R.id.btn_search_handwashes);
        recycler_search_results = findViewById(R.id.recycler_search_results);
        recycler_search_results.setLayoutManager(new LinearLayoutManager(this));

        btn_logout = findViewById(R.id.btn_logout);
        btn_delete_data = findViewById(R.id.btn_delete_data);
        btn_import_employees = findViewById(R.id.btn_import_employees);

        txt_message = findViewById(R.id.txt_message);

        edit_old_password = findViewById(R.id.edit_old_password);
        edit_new_password = findViewById(R.id.edit_new_password);
        edit_confirm_new_password = findViewById(R.id.edit_confirm_new_password);
        btn_change_password = findViewById(R.id.btn_change_password);

        edit_add_employee_number = findViewById(R.id.edit_add_employee_number);
        edit_add_first_name = findViewById(R.id.edit_add_first_name);
        edit_add_last_name = findViewById(R.id.edit_add_last_name);
        edit_add_department = findViewById(R.id.edit_add_department);
        btn_add_employee = findViewById(R.id.btn_add_employee);
    }

    private void loadAdminOverviewData() {
        Log.d(TAG, "Loading admin overview data...");
        if (dbHelper == null) { // Should not happen if initialized in onCreate
            dbHelper = new DatabaseHelper(this);
        }

        int totalWashesToday = dbHelper.getTotalHandwashesToday();
        txt_overview_total_washes_today.setText(String.format(Locale.getDefault(), "Total Handwashes Today: %d", totalWashesToday));

        int activeEmployees = dbHelper.getTotalActiveEmployeesCount();
        txt_overview_active_employees.setText(String.format(Locale.getDefault(), "Active Employees: %d", activeEmployees));

        List<LeaderboardEntry> topWashers = dbHelper.getTopHandwashers();
        if (!topWashers.isEmpty()) {
            LeaderboardEntry topWasher = topWashers.get(0);
            txt_overview_top_washer_today.setText(String.format(Locale.getDefault(), "Top Washer Today: %s (%d washes)",
                    topWasher.employeeName, topWasher.handwashCount));
        } else {
            txt_overview_top_washer_today.setText("Top Washer Today: N/A");
        }
        Log.d(TAG, "Admin overview data loaded.");
    }


    private void setupListeners() {
        btn_download_data.setOnClickListener(v -> downloadData());
        edit_download_start_date.setOnClickListener(v -> showDatePickerDialog(edit_download_start_date));
        edit_download_end_date.setOnClickListener(v -> showDatePickerDialog(edit_download_end_date));
        edit_search_start_date.setOnClickListener(v -> showDatePickerDialog(edit_search_start_date));
        edit_search_end_date.setOnClickListener(v -> showDatePickerDialog(edit_search_end_date));
        btn_search_handwashes.setOnClickListener(v -> searchHandwashes());
        btn_logout.setOnClickListener(v -> logout());
        btn_delete_data.setOnClickListener(v -> deleteDataWithConfirmation());
        btn_import_employees.setOnClickListener(v -> importEmployees());
        btn_change_password.setOnClickListener(v -> changeAdminPassword());
        btn_add_employee.setOnClickListener(v -> addEmployee());
    }

    private void showDatePickerDialog(final EditText editText) {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                AdminDashboardActivity.this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    String formattedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year1, monthOfYear + 1, dayOfMonth);
                    editText.setText(formattedDate);
                },
                year, month, day);
        datePickerDialog.show();
    }

    private void downloadData() {
        String startDate = edit_download_start_date.getText().toString();
        String endDate = edit_download_end_date.getText().toString();
        int selectedId = radio_download_type.getCheckedRadioButtonId();
        String downloadType;

        if (selectedId == R.id.radio_summary) {
            downloadType = "summary";
        } else if (selectedId == R.id.radio_detailed) {
            downloadType = "detailed";
        } else {
            txt_message.setText("Please select a download type.");
            Toast.makeText(this, "Please select a download type", Toast.LENGTH_SHORT).show();
            return;
        }

        if (startDate.isEmpty() || endDate.isEmpty()) {
            txt_message.setText("Please enter start and end dates.");
            Toast.makeText(this, "Please enter start and end dates.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<DatabaseHelper.HandwashLog> logs = dbHelper.getHandwashLogs(startDate, endDate, downloadType);
        if (logs.isEmpty()) {
            txt_message.setText("No data found for the selected criteria.");
            return;
        }

        String csvData = formatDataToCsv(logs, downloadType);
        try {
            File csvFile = createAndSaveCsvFile(csvData, downloadType, startDate, endDate);
            if (csvFile != null) {
                openCsvFile(csvFile);
                txt_message.setText("Data exported successfully to: " + csvFile.getName());
            } else {
                txt_message.setText("Error creating CSV file.");
            }
        } catch (IOException e) {
            txt_message.setText("Error saving data: " + e.getMessage());
            Log.e(TAG, "Error saving CSV", e);
        }
    }

    private String formatDataToCsv(List<DatabaseHelper.HandwashLog> logs, String downloadType) {
        StringBuilder csvBuilder = new StringBuilder();
        if (downloadType.equals("summary")) {
            csvBuilder.append("Employee Number,First Name,Last Name,Total Handwashes\n");
            for (DatabaseHelper.HandwashLog log : logs) {
                csvBuilder.append(log.employeeNumber).append(",")
                        .append(log.firstName == null ? "" : log.firstName).append(",")
                        .append(log.lastName == null ? "" : log.lastName).append(",")
                        .append(log.washCount).append("\n");
            }
        } else { // detailed
            csvBuilder.append("Employee Number,First Name,Last Name,Wash Date,Wash Time,Photo Path\n");
            for (DatabaseHelper.HandwashLog log : logs) {
                csvBuilder.append(log.employeeNumber).append(",")
                        .append(log.firstName == null ? "" : log.firstName).append(",")
                        .append(log.lastName == null ? "" : log.lastName).append(",")
                        .append(log.washDate).append(",")
                        .append(log.washTime).append(",")
                        .append(log.photoPath).append("\n");
            }
        }
        return csvBuilder.toString();
    }

    private File createAndSaveCsvFile(String csvData, String downloadType, String startDate, String endDate) throws IOException {
        String datePart = (!startDate.isEmpty() && !endDate.isEmpty()) ? startDate + "_to_" + endDate : "alltime";
        String fileName = "handwash_" + downloadType + "_" + datePart + "_" + System.currentTimeMillis() + ".csv";

        File documentsDir = getExternalFilesDir(null);
        if (documentsDir == null) {
            documentsDir = getFilesDir();
        }
        File exportDir = new File(documentsDir, "exports");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        File csvFile = new File(exportDir, fileName);
        Log.d(TAG, "Creating CSV file at: " + csvFile.getAbsolutePath());

        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write(csvData);
        }
        return csvFile;
    }


    private void openCsvFile(File csvFile) {
        Log.d(TAG, "Attempting to open CSV file: " + csvFile.getAbsolutePath());
        try {
            if (!csvFile.exists()) {
                txt_message.setText("CSV file does not exist.");
                Log.e(TAG, "CSV file does not exist: " + csvFile.getAbsolutePath());
                return;
            }
            Uri fileUri = FileProvider.getUriForFile(
                    this,
                    "com.jarindimick.handwashtracking.fileprovider",
                    csvFile
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, "text/csv");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(Intent.createChooser(intent, "Open CSV with"));
                Log.d(TAG, "Intent sent to open CSV file.");
            } else {
                txt_message.setText("No app found to open CSV file.");
                Log.e(TAG, "No app found to open CSV file.");
            }

        } catch (IllegalArgumentException e) {
            txt_message.setText("FileProvider error. Ensure provider_paths.xml is correct for exports directory.");
            Log.e(TAG, "FileProvider error", e);
        } catch (Exception e) {
            txt_message.setText("Error opening CSV file: " + e.getMessage());
            Log.e(TAG, "General error opening file", e);
        }
    }

    private void searchHandwashes() {
        String firstName = edit_search_first_name.getText().toString().trim();
        String lastName = edit_search_last_name.getText().toString().trim();
        String employeeId = edit_search_employee_id.getText().toString().trim();
        String startDate = edit_search_start_date.getText().toString().trim();
        String endDate = edit_search_end_date.getText().toString().trim();

        if (firstName.isEmpty() && lastName.isEmpty() && employeeId.isEmpty() && startDate.isEmpty() && endDate.isEmpty()) {
            txt_message.setText("Please enter at least one search criterion or a date range.");
            recycler_search_results.setVisibility(View.GONE);
            return;
        }

        List<DatabaseHelper.HandwashLog> results = dbHelper.searchHandwashLogs(firstName, lastName, employeeId, startDate, endDate);

        if (results.isEmpty()) {
            txt_message.setText("No handwash logs found matching the search criteria.");
            recycler_search_results.setVisibility(View.GONE);
        } else {
            txt_message.setText(String.format(Locale.getDefault(),"Found %d handwash log(s).", results.size()));
            recycler_search_results.setVisibility(View.VISIBLE);
            handwashLogAdapter = new HandwashLogAdapter(results, this);
            recycler_search_results.setAdapter(handwashLogAdapter);
        }
    }

    private void logout() {
        Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(AdminDashboardActivity.this, MainHandwashing.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void deleteDataWithConfirmation() {
        String startDate = edit_download_start_date.getText().toString();
        String endDate = edit_download_end_date.getText().toString();

        if (startDate.isEmpty() || endDate.isEmpty()) {
            txt_message.setText("Please enter start and end dates for deletion.");
            Toast.makeText(this, "Please enter start and end dates for deletion.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete handwash logs between " + startDate + " and " + endDate + "? This action cannot be undone.")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    int rowsDeleted = dbHelper.deleteHandwashLogs(startDate, endDate);
                    txt_message.setText(String.format(Locale.getDefault(),"Deleted %d handwash log(s).", rowsDeleted));
                    loadAdminOverviewData();
                    if (handwashLogAdapter != null) {
                        searchHandwashes();
                    }
                })
                .setNegativeButton(android.R.string.no, (dialog, which) -> {
                    txt_message.setText("Deletion cancelled.");
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void importEmployees() {
        String csvFileName = "employees.csv";
        List<Employee> employeesToImport;
        try {
            employeesToImport = readEmployeesFromCsvAsset(csvFileName);
        } catch (IOException e) {
            txt_message.setText("Error reading employees.csv from assets: " + e.getMessage());
            Log.e(TAG, "Error reading CSV", e);
            return;
        }

        int importedCount = 0;
        int errorCount = 0;

        for (Employee employee : employeesToImport) {
            long result = dbHelper.insertEmployee(employee.employeeNumber, employee.firstName, employee.lastName, "Imported");

            if (result > 0) {
                importedCount++;
            } else {
                errorCount++;
            }
        }
        txt_message.setText(String.format(Locale.getDefault(), "Processed %d employees. New/Updated: %d, Errors/Skipped: %d",
                employeesToImport.size(), importedCount, errorCount));
        Toast.makeText(this, "Employee import process completed.", Toast.LENGTH_SHORT).show();
        loadAdminOverviewData();
    }

    private List<Employee> readEmployeesFromCsvAsset(String csvFileName) throws IOException {
        List<Employee> employees = new ArrayList<>();
        AssetManager assetManager = getAssets();
        try (InputStream inputStream = assetManager.open(csvFileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            boolean isFirstLine = true;
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                String[] tokens = line.split(",");
                if (tokens.length >= 3) {
                    String employeeNumber = tokens[0].trim();
                    String firstName = tokens[1].trim();
                    String lastName = tokens[2].trim();
                    employees.add(new Employee(employeeNumber, firstName, lastName));
                } else {
                    Log.w(TAG, "Skipping malformed CSV line: " + line);
                }
            }
        }
        return employees;
    }


    private void changeAdminPassword() {
        String oldPassword = edit_old_password.getText().toString();
        String newPassword = edit_new_password.getText().toString();
        String confirmNewPassword = edit_confirm_new_password.getText().toString();

        if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
            txt_message.setText("Please fill in all password fields.");
            return;
        }
        if (newPassword.length() < 6) {
            txt_message.setText("New password must be at least 6 characters.");
            return;
        }
        if (!newPassword.equals(confirmNewPassword)) {
            txt_message.setText("New passwords do not match.");
            return;
        }

        if (dbHelper.validateAdminLogin("admin", oldPassword)) {
            boolean updated = dbHelper.updateAdminPassword("admin", newPassword);
            if (updated) {
                txt_message.setText("Password changed successfully.");
                edit_old_password.setText("");
                edit_new_password.setText("");
                edit_confirm_new_password.setText("");
            } else {
                txt_message.setText("Error updating password.");
            }
        } else {
            txt_message.setText("Incorrect old password.");
        }
    }

    private void addEmployee() {
        String employeeNumber = edit_add_employee_number.getText().toString().trim();
        String firstName = edit_add_first_name.getText().toString().trim();
        String lastName = edit_add_last_name.getText().toString().trim();
        String department = edit_add_department.getText().toString().trim();

        if (employeeNumber.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
            txt_message.setText("Employee Number, First Name, and Last Name are required.");
            return;
        }
        if (department.isEmpty()) {
            department = "Unassigned";
        }

        if (dbHelper.isEmployeeNumberTaken(employeeNumber)) {
            txt_message.setText("Employee number already exists. Please use a unique number.");
            return;
        }

        long result = dbHelper.insertEmployee(employeeNumber, firstName, lastName, department);
        if (result != -1) {
            txt_message.setText("Employee added successfully: " + firstName + " " + lastName);
            edit_add_employee_number.setText("");
            edit_add_first_name.setText("");
            edit_add_last_name.setText("");
            edit_add_department.setText("");
            loadAdminOverviewData();
        } else {
            txt_message.setText("Error adding employee. Ensure employee number is unique.");
        }
    }

    private static class Employee {
        String employeeNumber;
        String firstName;
        String lastName;

        public Employee(String employeeNumber, String firstName, String lastName) {
            this.employeeNumber = employeeNumber;
            this.firstName = firstName;
            this.lastName = lastName;
        }
    }

    // Helper to convert dp to pixels (if needed for manual padding adjustments)
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // dbHelper is closed by individual methods in DatabaseHelper.
    }
}
