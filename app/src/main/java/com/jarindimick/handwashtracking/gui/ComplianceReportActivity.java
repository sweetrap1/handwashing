package com.jarindimick.handwashtracking.gui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.jarindimick.handwashtracking.R;
import com.jarindimick.handwashtracking.databasehelper.DatabaseHelper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ComplianceReportActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView txtNoData;
    private DatabaseHelper dbHelper;
    private ComplianceReportAdapter adapter;

    // New UI Elements
    private TextView txtSelectedDate;
    private Button btnChangeDate;
    private Spinner spinnerDepartment;
    private EditText editSearchEmployeeName;
    private Button btnFilterSearch;

    // State holder for the selected date
    private LocalDate selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compliance_report);

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
        dbHelper = new DatabaseHelper(this);

        // Set initial date to today
        selectedDate = LocalDate.now();

        setupFilters();
        updateDateDisplay();
        loadReportData(); // Load initial data for today

        // Set listeners
        btnChangeDate.setOnClickListener(v -> showDatePickerDialog());
        btnFilterSearch.setOnClickListener(v -> loadReportData());
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
                    loadReportData(); // Refresh data with the new date
                },
                selectedDate.getYear(),
                selectedDate.getMonthValue() - 1,
                selectedDate.getDayOfMonth());
        datePickerDialog.show();
    }

    private void updateDateDisplay() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.getDefault());
        txtSelectedDate.setText(String.format("Date: %s", selectedDate.format(formatter)));
    }

    private void loadReportData() {
        // Get current filter values
        String dept = spinnerDepartment.getSelectedItem().toString();
        if (dept.equals("All Departments")) {
            dept = null;
        }
        String name = editSearchEmployeeName.getText().toString().trim();

        // Call the database with the selected date and filters
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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}