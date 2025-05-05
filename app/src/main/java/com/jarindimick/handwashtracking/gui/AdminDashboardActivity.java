package com.jarindimick.handwashtracking.gui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.jarindimick.handwashtracking.R;

import java.util.Calendar;

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

    // UI elements for Other Buttons
    private Button btn_logout;
    private Button btn_delete_data;
    private Button btn_import_employees;

    // Message Display
    private TextView txt_message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        setupgui();
        setupListeners();
    }

    private void setupgui() {
        // Download Data
        edit_download_start_date = findViewById(R.id.edit_download_start_date);
        edit_download_end_date = findViewById(R.id.edit_download_end_date);
        radio_download_type = findViewById(R.id.radio_download_type);
        btn_download_data = findViewById(R.id.btn_download_data);

        // Search Handwashes
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

        // Message Display
        txt_message = findViewById(R.id.txt_message);
    }

    private void setupListeners() {
        btn_download_data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadData();
            }
        });

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

        btn_search_handwashes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Implement Search Handwashes logic here (LATER)
                Toast.makeText(AdminDashboardActivity.this, "Search Handwashes clicked", Toast.LENGTH_SHORT).show();
            }
        });

        btn_logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Implement Logout logic here (LATER)
                Toast.makeText(AdminDashboardActivity.this, "Logout clicked", Toast.LENGTH_SHORT).show();
                //  For now, just finish the activity (go back to login)
                finish();
            }
        });

        btn_delete_data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Implement Delete Data logic here (LATER)
                Toast.makeText(AdminDashboardActivity.this, "Delete Data clicked", Toast.LENGTH_SHORT).show();
            }
        });

        btn_import_employees.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Implement Import Employees logic here (LATER)
                Toast.makeText(AdminDashboardActivity.this, "Import Employees clicked", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void downloadData() {
        String startDate = edit_download_start_date.getText().toString();
        String endDate = edit_download_end_date.getText().toString();
        String downloadType = radio_download_type.getCheckedRadioButtonId() == R.id.radio_summary ? "summary" : "detailed";

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
        // makeAPICall(apiUrl);

        // Display a success message (replace with actual response handling later)
        txt_message.append("\nData download initiated (replace with actual result)");
    }

    // Placeholder for API call (IMPLEMENT LATER)
    private void makeAPICall(String url) {
        // Implement your network request here (e.g., using HttpURLConnection, Retrofit, etc.)
        // This is a placeholder; replace with actual network code
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
                        String formattedDate = String.format("%04d-%02d-%02d", year, monthOfYear + 1, dayOfMonth);
                        editText.setText(formattedDate);
                    }
                },
                year, month, day);
        datePickerDialog.show();
    }
}