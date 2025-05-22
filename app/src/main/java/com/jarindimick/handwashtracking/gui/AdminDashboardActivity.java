package com.jarindimick.handwashtracking.gui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.jarindimick.handwashtracking.R;
import com.jarindimick.handwashtracking.databasehelper.DatabaseHelper;

import org.mindrot.jbcrypt.BCrypt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AdminDashboardActivity extends AppCompatActivity {

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

    // UI elements for adding new employee // New Declarations
    private EditText edit_add_employee_number;
    private EditText edit_add_first_name;
    private EditText edit_add_last_name;
    private EditText edit_add_department;
    private Button btn_add_employee;


    // Declare DatabaseHelper if needed in this Activity
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);
        // getSupportActionBar().hide(); // You can keep or remove this based on preference
        setupgui();
        setupListeners();
        dbHelper = new DatabaseHelper(this);
    }

    private void setupgui() {
        // Admin Dashboard Title
        TextView lbl_admin_dashboard_title = findViewById(R.id.lbl_admin_dashboard_title);


        // Download Data
        edit_download_start_date = findViewById(R.id.edit_download_start_date);
        edit_download_end_date = findViewById(R.id.edit_download_end_date);
        radio_download_type = findViewById(R.id.radio_download_type);
        btn_download_data = findViewById(R.id.btn_download_data);

        // Search Handwashes
        TextView lbl_search_handwashes_title = findViewById(R.id.lbl_search_handwashes_title);
        edit_search_first_name = findViewById(R.id.edit_search_first_name);
        edit_search_last_name = findViewById(R.id.edit_search_last_name);
        edit_search_employee_id = findViewById(R.id.edit_search_employee_id);
        edit_search_start_date = findViewById(R.id.edit_search_start_date);
        edit_search_end_date = findViewById(R.id.edit_search_end_date);
        btn_search_handwashes = findViewById(R.id.btn_search_handwashes);

        // Other Buttons
        btn_logout = findViewById(R.id.btn_logout);
        btn_delete_data = findViewById(R.id.btn_delete_data);
        btn_import_employees = findViewById(R.id.btn_import_employees);

        // Text message area
        TextView lbl_message_area_title = findViewById(R.id.lbl_message_area_title);
        txt_message = findViewById(R.id.txt_message);

        // Change Password UI elements
        TextView lbl_change_password_title = findViewById(R.id.lbl_change_password_title);
        edit_old_password = findViewById(R.id.edit_old_password);
        edit_new_password = findViewById(R.id.edit_new_password);
        edit_confirm_new_password = findViewById(R.id.edit_confirm_new_password);
        btn_change_password = findViewById(R.id.btn_change_password);

        // Add New Employee UI elements // New Initializations
        TextView lbl_add_employee_title = findViewById(R.id.lbl_add_employee_title);
        edit_add_employee_number = findViewById(R.id.edit_add_employee_number);
        edit_add_first_name = findViewById(R.id.edit_add_first_name);
        edit_add_last_name = findViewById(R.id.edit_add_last_name);
        edit_add_department = findViewById(R.id.edit_add_department);
        btn_add_employee = findViewById(R.id.btn_add_employee);


        //Hide action bar - you can keep or remove this based on preference
        // getSupportActionBar().hide();
    }

    private void setupListeners() {
        // Listener for Download Data Button
        btn_download_data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadData();
            }
        });

        // Listeners for Date EditTexts to show DatePickerDialog
        edit_download_start_date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(edit_download_start_date);
            }
        });

        edit_download_end_date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(edit_download_end_date);
            }
        });

        edit_search_start_date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(edit_search_start_date);
            }
        });

        edit_search_end_date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(edit_search_end_date);
            }
        });

        // Listeners for the remaining buttons
        btn_search_handwashes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchHandwashes();
            }
        });

        btn_logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });

        btn_delete_data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteData();
            }
        });

        btn_import_employees.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                importEmployees();
            }
        });

        btn_change_password.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeAdminPassword();
            }
        });

        // Listener for Add Employee Button // New Listener
        btn_add_employee.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addEmployee();
            }
        });
    }

    private void showDatePickerDialog(final EditText editText) {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                AdminDashboardActivity.this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        String formattedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, monthOfYear + 1, dayOfMonth);
                        editText.setText(formattedDate);
                    }
                },
                year, month, day);
        datePickerDialog.show();
    }

    private void downloadData() {
        String startDate = edit_download_start_date.getText().toString();
        String endDate = edit_download_end_date.getText().toString();

        // Get selected download type from RadioGroup
        int selectedId = radio_download_type.getCheckedRadioButtonId();
        String downloadType;
        if (selectedId != -1) {
            if (selectedId == R.id.radio_summary) {
                downloadType = "summary";
            } else if (selectedId == R.id.radio_detailed) {
                downloadType = "detailed";
            } else {
                downloadType = "unknown";
                Toast.makeText(this, "Please select a download type", Toast.LENGTH_SHORT).show();
                txt_message.setText("Please select a download type.");
                return;
            }
        } else {
            Toast.makeText(this, "Please select a download type", Toast.LENGTH_SHORT).show();
            txt_message.setText("Please select a download type.");
            return;
        }

        // Basic input validation
        if (startDate.isEmpty() || endDate.isEmpty()) {
            txt_message.setText("Please enter start and end dates.");
            return;
        }

        List<DatabaseHelper.HandwashLog> logs = dbHelper.getHandwashLogs(startDate, endDate, downloadType);
        if (logs.isEmpty()) {
            txt_message.setText("No data found for the selected criteria.");
            return;
        }

        String csvData = formatDataToCsv(logs, downloadType);
        try {
            File csvFile = createAndSaveCsvFile(csvData, downloadType);
            if (csvFile != null) {
                openCsvFile(csvFile);
            } else {
                txt_message.setText("Error creating CSV file.");
            }
        } catch (IOException e) {
            txt_message.setText("Error saving data: " + e.getMessage());
            Log.e("DownloadData", "Error saving CSV", e);
        }
    }

    private String formatDataToCsv(List<DatabaseHelper.HandwashLog> logs, String downloadType) {
        Log.d("formatDataToCsv", "Formatting data for download type: " + downloadType);
        StringBuilder csvBuilder = new StringBuilder();

        // Add header row
        if (downloadType.equals("summary")) {
            csvBuilder.append("Employee Number,Total Handwashes\n");
        } else if (downloadType.equals("detailed")) {
            csvBuilder.append("Employee Number,Wash Date,Wash Time,Photo Path\n");
        } else {
            Log.e("formatDataToCsv", "Unknown download type: " + downloadType);
            return ""; // Or throw an exception
        }

        for (DatabaseHelper.HandwashLog log : logs) {
            csvBuilder.append(log.employeeNumber).append(",");
            if (downloadType.equals("summary")) {
                csvBuilder.append(log.washCount).append("\n");
            } else if (downloadType.equals("detailed")) {
                csvBuilder.append(log.washDate).append(",").append(log.washTime).append(",").append(log.photoPath).append("\n");
            } else {
                Log.e("formatDataToCsv", "Unknown download type in loop: " + downloadType);
            }
        }

        Log.d("formatDataToCsv", "CSV Data:\n" + csvBuilder.toString());
        return csvBuilder.toString();
    }

    private File createAndSaveCsvFile(String csvData, String downloadType) throws IOException {
        String fileName = "handwash_data_" + (downloadType.equals("summary") ? "summary" : "detailed") + "_" +
                System.currentTimeMillis() + ".csv";
        File csvFile = new File(getCacheDir(), fileName); // Save to app's cache directory

        Log.d("createAndSaveCsvFile", "Creating file: " + csvFile.getAbsolutePath());

        FileWriter writer = new FileWriter(csvFile);
        writer.write(csvData);
        writer.close();

        Log.d("createAndSaveCsvFile", "File created successfully.");
        return csvFile;
    }

    private void openCsvFile(File csvFile) {
        Log.d("openCsvFile", "Attempting to open CSV file: " + csvFile.getAbsolutePath());
        try {
            if (!csvFile.exists()) {
                txt_message.setText("CSV file does not exist.");
                Log.e("openCsvFile", "CSV file does not exist: " + csvFile.getAbsolutePath());
                return;
            }

            // Use FileProvider to share the file (for Android 7.0 and above)
            Uri fileUri = FileProvider.getUriForFile(
                    this,
                    "com.jarindimick.handwashtracking.fileprovider", // Make sure this matches your manifest!
                    csvFile
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, "text/csv"); // Explicitly set type and data
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Verify that there's an app to handle the Intent
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(Intent.createChooser(intent, "Open with"));
                Log.d("openCsvFile", "Intent sent to open CSV file.");
            } else {
                txt_message.setText("No app found to open CSV file.");
                Log.e("openCsvFile", "No app found to open CSV file.");
            }

        } catch (IllegalArgumentException e) {
            txt_message.setText("FileProvider error: " + e.getMessage());
            Log.e("openCsvFile", "FileProvider error", e);
        } catch (Exception e) {
            txt_message.setText("Error opening CSV file: " + e.getMessage());
            Log.e("openCsvFile", "General error opening file", e);
        }
    }

    private void changeAdminPassword() {
        String oldPassword = edit_old_password.getText().toString();
        String newPassword = edit_new_password.getText().toString();
        String confirmNewPassword = edit_confirm_new_password.getText().toString();

        if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
            txt_message.setText("Please fill in all password fields.");
            return;
        }

        if (!newPassword.equals(confirmNewPassword)) {
            txt_message.setText("New passwords do not match.");
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT " + DatabaseHelper.COLUMN_PASSWORD_HASH + " FROM " + DatabaseHelper.TABLE_ADMIN_USERS +
                " WHERE " + DatabaseHelper.COLUMN_USERNAME + " = 'admin'";
        Cursor cursor = db.rawQuery(query, null);
        String storedHash = null;
        if (cursor.moveToFirst()) {
            storedHash = cursor.getString(0);
        }
        cursor.close();
        db.close();

        if (storedHash != null && BCrypt.checkpw(oldPassword, storedHash)) {
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

    private void searchHandwashes() {
        String firstName = edit_search_first_name.getText().toString();
        String lastName = edit_search_last_name.getText().toString();
        String employeeId = edit_search_employee_id.getText().toString();
        String startDate = edit_search_start_date.getText().toString();
        String endDate = edit_search_end_date.getText().toString();

        List<DatabaseHelper.HandwashLog> results = dbHelper.searchHandwashLogs(firstName, lastName, employeeId, startDate, endDate);

        if (results.isEmpty()) {
            txt_message.setText("No handwash logs found matching the search criteria.");
        } else {
            StringBuilder message = new StringBuilder("Search Results:\n");
            for (DatabaseHelper.HandwashLog log : results) {
                message.append("--------------------\n");
                message.append("Employee Number: ").append(log.employeeNumber).append("\n");
                message.append("Wash Date: ").append(log.washDate).append("\n");
                message.append("Wash Time: ").append(log.washTime).append("\n");
                message.append("Photo Path: ").append(log.photoPath).append("\n");
            }
            txt_message.setText(message.toString());
        }
    }

    private void logout() {
        Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(AdminDashboardActivity.this, MainHandwashing.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void deleteData() {
        String startDate = edit_download_start_date.getText().toString();
        String endDate = edit_download_end_date.getText().toString();

        if (startDate.isEmpty() || endDate.isEmpty()) {
            txt_message.setText("Please enter start and end dates.");
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete handwash logs between " + startDate + " and " + endDate + "? This action cannot be undone.")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    int rowsDeleted = dbHelper.deleteHandwashLogs(startDate, endDate);
                    if (rowsDeleted >= 0) {
                        txt_message.setText("Deleted " + rowsDeleted + " handwash logs.");
                    } else {
                        txt_message.setText("Error deleting handwash logs.");
                    }
                })
                .setNegativeButton(android.R.string.no, (dialog, which) -> {
                    txt_message.setText("Deletion cancelled.");
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void importEmployees() {
        String csvFileName = "employees.csv"; // The name of your CSV file in assets

        List<Employee> employees;
        try {
            employees = readAndParseCsvFile(csvFileName);
        } catch (IOException e) {
            txt_message.setText("Error reading CSV file: " + e.getMessage());
            Log.e("ImportEmployees", "Error reading CSV", e);
            return;
        }

        int importedCount = 0;
        int errorCount = 0;
        for (Employee employee : employees) {
            // Assuming your CSV has Employee Number, First Name, Last Name, and potentially Department
            // Adjust the Employee class and readAndParseCsvFile if your CSV format is different.
            // For this example, we'll call the new insertEmployee with a placeholder for department
            long result = dbHelper.insertEmployee(employee.employeeNumber, employee.firstName, employee.lastName, "Imported"); // Use a placeholder department
            if (result != -1) {
                importedCount++;
            } else {
                errorCount++; // Employee number might already exist
            }
        }

        txt_message.setText("Imported " + importedCount + " employees. Errors: " + errorCount);
        Toast.makeText(this, "Import completed.", Toast.LENGTH_SHORT).show();
    }

    private List<Employee> readAndParseCsvFile(String csvFileName) throws IOException {
        List<Employee> employees = new ArrayList<>();
        AssetManager assetManager = getAssets();
        InputStream inputStream = assetManager.open(csvFileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        boolean isFirstLine = true; // Skip header row

        while ((line = reader.readLine()) != null) {
            if (isFirstLine) {
                isFirstLine = false;
                continue;
            }
            String[] tokens = line.split(",");
            // Adjust the number of tokens based on your CSV structure
            // Assuming CSV format: Employee Number,First Name,Last Name
            if (tokens.length >= 3) {
                String employeeNumber = tokens[0].trim();
                String firstName = tokens[1].trim();
                String lastName = tokens[2].trim();
                // If your CSV has a department, you would add it here:
                // String department = tokens.length > 3 ? tokens[3].trim() : "";
                // employees.add(new Employee(employeeNumber, firstName, lastName, department));
                employees.add(new Employee(employeeNumber, firstName, lastName)); // Using the simplified Employee class for now
            } else {
                Log.w("CSV Parser", "Skipping line: " + line + " (Not enough columns)");
            }
        }
        reader.close();
        inputStream.close();
        return employees;
    }

    // Simplified Employee class for CSV import if department is not in CSV
    private static class Employee {
        String employeeNumber;
        String firstName;
        String lastName;
        // String department; // Include if department is in CSV

        public Employee(String employeeNumber, String firstName, String lastName) {
            this.employeeNumber = employeeNumber;
            this.firstName = firstName;
            this.lastName = lastName;
        }
        // Add constructor with department if needed
        /*
        public Employee(String employeeNumber, String firstName, String lastName, String department) {
            this.employeeNumber = employeeNumber;
            this.firstName = firstName;
            this.lastName = lastName;
            this.department = department;
        }
        */
    }


    // New method to add employee manually // New Method
    private void addEmployee() {
        String employeeNumber = edit_add_employee_number.getText().toString().trim();
        String firstName = edit_add_first_name.getText().toString().trim();
        String lastName = edit_add_last_name.getText().toString().trim();
        String department = edit_add_department.getText().toString().trim();

        if (employeeNumber.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || department.isEmpty()) {
            txt_message.setText("Please fill all employee details.");
            return;
        }

        long result = dbHelper.insertEmployee(employeeNumber, firstName, lastName, department);

        if (result != -1) {
            txt_message.setText("Employee added successfully.");
            // Clear the input fields
            edit_add_employee_number.setText("");
            edit_add_first_name.setText("");
            edit_add_last_name.setText("");
            edit_add_department.setText("");
        } else {
            txt_message.setText("Error adding employee. Employee number might already exist.");
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}