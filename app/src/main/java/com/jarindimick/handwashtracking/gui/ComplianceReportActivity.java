package com.jarindimick.handwashtracking.gui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log; // Added for logging isFreeVersion
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jarindimick.handwashtracking.R;
import com.jarindimick.handwashtracking.databasehelper.DatabaseHelper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import androidx.activity.EdgeToEdge;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ComplianceReportActivity extends AppCompatActivity {

    private static final String TAG = "ComplianceReport";
    private RecyclerView recyclerView;
    private TextView txtNoData;
    private DatabaseHelper dbHelper;
    private ComplianceReportAdapter adapter;

    private TextView txtSelectedDate;
    private Button btnChangeDate;
    private Spinner spinnerDepartment;
    private EditText editSearchEmployeeName;
    private Button btnFilterSearch;

    private LocalDate selectedDate;
    private boolean isFreeVersion; // Declare isFreeVersion flag

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Enable Edge-to-Edge display
        EdgeToEdge.enable(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compliance_report);

        // Determine if this is the free version based on the build flavor
        isFreeVersion = getApplicationContext().getPackageName().endsWith(".free");
        Log.d(TAG, "isFreeVersion: " + isFreeVersion);

        // Find the root layout
        ConstraintLayout mainLayout = findViewById(R.id.main);

        // Apply insets to handle system bars (status bar, navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar_compliance_report);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize UI elements
        recyclerView = findViewById(R.id.recycler_compliance_report);
        txtNoData = findViewById(R.id.txt_no_data_report);
        txtSelectedDate = findViewById(R.id.txt_selected_date);
        btnChangeDate = findViewById(R.id.btn_change_date);
        spinnerDepartment = findViewById(R.id.spinner_department);
        editSearchEmployeeName = findViewById(R.id.edit_search_employee_name);
        btnFilterSearch = findViewById(R.id.btn_filter_search);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Initialize DatabaseHelper with the isFreeVersion flag
        dbHelper = new DatabaseHelper(this, isFreeVersion);

        // Set initial date to today
        selectedDate = LocalDate.now();

        setupFilters();
        updateDateDisplay();
        loadReportData();

        // Set listeners
        btnChangeDate.setOnClickListener(v -> showDatePickerDialog());
        btnFilterSearch.setOnClickListener(v -> loadReportData());
    }

    // --- NEW METHOD to create and show the menu ---
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.compliance_report_menu, menu);
        return true;
    }

    // --- NEW METHOD to handle menu item clicks ---
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.action_share_report) {
            shareReport();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // --- NEW METHOD to generate and share the CSV ---
    private void shareReport() {
        if (adapter == null || adapter.getItemCount() == 0) {
            Toast.makeText(this, "There is no report data to share.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<DatabaseHelper.ComplianceResult> reportData = adapter.getReportData();
        StringBuilder csvBuilder = new StringBuilder();
        // Add header row
        csvBuilder.append("Employee Name,Employee Number,Hour,Compliance Status\n");

        // Add data rows
        for (DatabaseHelper.ComplianceResult result : reportData) {
            if (result.hourlyStatus.isEmpty()) {
                csvBuilder.append(String.format("\"%s\",%s,N/A,No activity during shift\n", result.employeeName, result.employeeNumber));
            } else {
                for (java.util.Map.Entry<Integer, Boolean> entry : result.hourlyStatus.entrySet()) {
                    String hourFormatted = String.format(Locale.getDefault(), "%d:00 - %d:59", entry.getKey(), entry.getKey());
                    String status = entry.getValue() ? "Compliant" : "NOT Compliant";
                    csvBuilder.append(String.format("\"%s\",%s,%s,%s\n", result.employeeName, result.employeeNumber, hourFormatted, status));
                }
            }
        }

        // Save to a file and trigger share intent
        try {
            String fileName = "compliance_report_" + selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".csv";
            File exportsDir = new File(getExternalFilesDir(null), "exports");
            if (!exportsDir.exists()) {
                exportsDir.mkdirs();
            }
            File file = new File(exportsDir, fileName);

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(csvBuilder.toString());
            }

            Uri fileUri = FileProvider.getUriForFile(this, "com.jarindimick.handwashtracking.fileprovider", file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/csv");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Handwash Compliance Report for " + selectedDate.toString());
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share Report Via"));

        } catch (IOException | IllegalArgumentException e) {
            Log.e(TAG, "Error saving or sharing compliance CSV", e);
            Toast.makeText(this, "Error sharing file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupFilters() {
        List<String> departments = dbHelper.getAllDepartments();
        departments.add(0, "All Departments");

        ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, departments);
        deptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDepartment.setAdapter(deptAdapter);

        spinnerDepartment.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadReportData();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
                    updateDateDisplay();
                    loadReportData();
                },
                selectedDate.getYear(),
                selectedDate.getMonthValue() - 1,
                selectedDate.getDayOfMonth());
        datePickerDialog.show();
    }

    private void updateDateDisplay() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd,yyyy", Locale.getDefault());
        txtSelectedDate.setText(String.format("Date: %s", selectedDate.format(formatter)));
    }

    private void loadReportData() {
        String dept = spinnerDepartment.getSelectedItem().toString();
        if (dept.equals("All Departments")) {
            dept = null;
        }
        String name = editSearchEmployeeName.getText().toString().trim();

        List<DatabaseHelper.ComplianceResult> reportData = dbHelper.getHourlyComplianceForDate(selectedDate, dept, name);

        if (reportData == null || reportData.isEmpty()) {
            txtNoData.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            txtNoData.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter = new ComplianceReportAdapter(reportData);
            recyclerView.setAdapter(adapter);
        }
    }
}
