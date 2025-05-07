package com.jarindimick.handwashtracking.gui;

import android.app.DatePickerDialog;
import android.content.Intent; // Needed for logout or other navigation
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.jarindimick.handwashtracking.R;
import com.jarindimick.handwashtracking.databasehelper.DatabaseHelper;

import org.mindrot.jbcrypt.BCrypt;

import java.util.Calendar;
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

    // Declare DatabaseHelper if needed in this Activity
    private DatabaseHelper dbHelper; //  IMPORTANT:  Declare it here

    // UI elements for changing admin password
    private EditText edit_old_password;
    private EditText edit_new_password;
    private EditText edit_confirm_new_password;
    private Button btn_change_password;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard); // Make sure this layout file exists
        getSupportActionBar().hide();
        setupgui();
        setupListeners();
        // Initialize dbHelper here if uncommented above
        dbHelper = new DatabaseHelper(this);  // IMPORTANT: Initialize it in onCreate
    }

    private void setupgui() {
        // Download Data
        edit_download_start_date = findViewById(R.id.edit_download_start_date);
        edit_download_end_date = findViewById(R.id.edit_download_end_date);
        radio_download_type = findViewById(R.id.radio_download_type); // Make sure R.id.radio_download_type is a RadioGroup
        btn_download_data = findViewById(R.id.btn_download_data);

        // Search Handwashes
        edit_search_first_name = findViewById(R.id.edit_search_first_name); // Make sure these IDs exist in your layout
        edit_search_last_name = findViewById(R.id.edit_search_last_name);
        edit_search_employee_id = findViewById(R.id.edit_search_employee_id);
        edit_search_start_date = findViewById(R.id.edit_search_start_date);
        edit_search_end_date = findViewById(R.id.edit_search_end_date);
        btn_search_handwashes = findViewById(R.id.btn_search_handwashes);

        // Other Buttons
        btn_logout = findViewById(R.id.btn_logout); // Make sure these IDs exist in your layout
        btn_delete_data = findViewById(R.id.btn_delete_data);
        btn_import_employees = findViewById(R.id.btn_import_employees);

        // Text Buttons (Assuming this was meant to be a TextView message area)
        txt_message = findViewById(R.id.txt_message); // Make sure this ID exists in your layout and is a TextView

        // Change Password UI elements
        edit_old_password = findViewById(R.id.edit_old_password);
        edit_new_password = findViewById(R.id.edit_new_password);
        edit_confirm_new_password = findViewById(R.id.edit_confirm_new_password);
        btn_change_password = findViewById(R.id.btn_change_password);
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

        // Listeners for the remaining buttons (Search, Logout, Delete, Import)

        btn_search_handwashes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchHandwashes(); // Call the search method
            }
        });

        btn_logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout(); // Call the logout method
            }
        });

        btn_delete_data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteData(); // Call the delete method
            }
        });

        btn_import_employees.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                importEmployees(); // Call the import method
            }
        });
        btn_change_password.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeAdminPassword();
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
                        // Format the date as YYYY-MM-DD
                        // monthOfYear is 0-indexed, so add 1
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
        if (selectedId != -1) { // Check if a radio button is selected
            if (selectedId == R.id.radio_summary) { // Assuming R.id.radio_summary is the ID for the summary radio button
                downloadType = "summary";
            } else if (selectedId == R.id.radio_detailed) { // Assuming R.id.radio_detailed is the ID for the detailed radio button
                downloadType = "detailed";
            } else {
                downloadType = "unknown"; // Default or error case
                Toast.makeText(this, "Please select a download type", Toast.LENGTH_SHORT).show();
                txt_message.setText("Please select a download type.");
                return; // Exit if no valid type selected
            }
        } else {
            Toast.makeText(this, "Please select a download type", Toast.LENGTH_SHORT).show();
            txt_message.setText("Please select a download type.");
            return; // Exit if no type is selected
        }


        // Basic input validation
        if (startDate.isEmpty() || endDate.isEmpty()) {
            txt_message.setText("Please enter start and end dates.");
            return;
        }

        // *** REPLACE THIS WITH ACTUAL API CALL LATER ***
        String apiUrl = "your_api_url/download_data.php" +  // Replace with your actual API URL
                "?start_date=" + startDate +
                "&end_date=" + endDate +
                "&download_type=" + downloadType;

        // For now, just display the constructed URL and a success message
        txt_message.setText("Downloading data from: " + apiUrl);
        // Simulate a successful download for now
        // In the future you would make an API call here and handle the response
        // Example:
        // makeAPICall(apiUrl); // Call the placeholder API method

        // Display a success message (replace with actual response handling later)
        txt_message.append("\nData download initiated (replace with actual result)");
    }

    // --- Placeholder Methods (Implement the actual logic for these) ---

    private void changeAdminPassword() {
        String oldPassword = edit_old_password.getText().toString();
        String newPassword = edit_new_password.getText().toString();
        String confirmNewPassword = edit_confirm_new_password.getText().toString();

        if (!newPassword.equals(confirmNewPassword)) {
            txt_message.setText("New passwords do not match.");
            return;
        }

        // Verify the old password (using the same logic as in AdminLoginActivity)
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT " + DatabaseHelper.COLUMN_PASSWORD_HASH + " FROM " + DatabaseHelper.TABLE_ADMIN_USERS +
                " WHERE " + DatabaseHelper.COLUMN_USERNAME + " = 'admin'";  // Assuming 'admin' is the username
        Cursor cursor = db.rawQuery(query, null);
        String storedHash = null;
        if (cursor.moveToFirst()) {
            storedHash = cursor.getString(0);
        }
        cursor.close();
        db.close();

        if (storedHash != null && BCrypt.checkpw(oldPassword, storedHash)) {
            // Old password is correct, update the password
            boolean updated = dbHelper.updateAdminPassword("admin", newPassword); // Assuming 'admin' is the username
            if (updated) {
                txt_message.setText("Password changed successfully.");
            } else {
                txt_message.setText("Error updating password.");
            }
        } else {
            txt_message.setText("Incorrect old password.");
        }
    }

    private void searchHandwashes() {
        // TODO: Implement logic to search handwashes based on input fields
        Toast.makeText(this, "Search Handwashes button clicked (Implement me!)", Toast.LENGTH_SHORT).show();
        // Example: Get search criteria from edit_search fields and query database/API
        String firstName = edit_search_first_name.getText().toString();
        String lastName = edit_search_last_name.getText().toString();
        String employeeId = edit_search_employee_id.getText().toString();
        String startDate = edit_search_start_date.getText().toString();
        String endDate = edit_search_end_date.getText().toString();

        // Use these variables to perform your search...
        txt_message.setText("Searching for:\nFirst Name: " + firstName + "\nLast Name: " + lastName +
                "\nEmployee ID: " + employeeId + "\nStart Date: " + startDate + "\nEnd Date: " + endDate);
        // ... perform search logic here ...
        txt_message.append("\nSearch initiated (replace with actual results)");
    }

    private void logout() {
        // TODO: Implement logout logic (clear session, credentials, etc.)
        Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();
        // Example: Navigate back to MainHandwashing or a login screen
        Intent intent = new Intent(AdminDashboardActivity.this, MainHandwashing.class);
        // Clear back stack so user can't go back to admin screen with back button
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Close the current activity
    }

    private void deleteData() {
        String startDate = edit_download_start_date.getText().toString();
        String endDate = edit_download_end_date.getText().toString();

        // Basic input validation
        if (startDate.isEmpty() || endDate.isEmpty()) {
            txt_message.setText("Please enter start and end dates.");
            return;
        }

        // Confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete handwash logs between " + startDate + " and " + endDate + "?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    // User clicked Yes, proceed with deletion
                    int rowsDeleted = dbHelper.deleteHandwashLogs(startDate, endDate);
                    if (rowsDeleted >= 0) {
                        txt_message.setText("Deleted " + rowsDeleted + " handwash logs.");
                    } else {
                        txt_message.setText("Error deleting handwash logs.");
                    }
                })
                .setNegativeButton(android.R.string.no, (dialog, which) -> {
                    // User clicked No, do nothing
                    txt_message.setText("Deletion cancelled.");
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void importEmployees() {
        // TODO: Implement logic to import employees
        Toast.makeText(this, "Import Employees button clicked (Implement me!)", Toast.LENGTH_SHORT).show();
        // Example: Open a file picker or read from a predefined location and insert into database/API
        txt_message.setText("Import Employees button clicked (Implement actual import logic)");
    }


    // Placeholder for API call (IMPLEMENT LATER)
    // Uncomment and implement this when you integrate networking
    /*
    private void makeAPICall(String url) {
        // Implement your network request here (e.g., using HttpURLConnection, Retrofit, Volley, etc.)
        // This is a placeholder; replace with actual network code
        // Example:
        // new Thread(() -> {
        //     try {
        //         URL apiEndpoint = new URL(url);
        //         HttpURLConnection myConnection = (HttpURLConnection) apiEndpoint.openConnection();
        //         // Set up connection properties, read response, handle errors, etc.
        //         // ...
        //         myConnection.disconnect();
        //     } catch (Exception e) {
        //         e.printStackTrace();
        //         // Handle exceptions
        //     }
        // }).start();
    }
    */

    // Add onDestroy if needed for cleanup (e.g., closing database helper)
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}