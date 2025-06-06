package com.jarindimick.handwashtracking.gui;

// Add necessary imports (many might already be there)
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu; // New import
import android.view.MenuInflater; // New import
import android.view.MenuItem; // New import
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable; // New import
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.jarindimick.handwashtracking.R;
import com.jarindimick.handwashtracking.databasehelper.DatabaseHelper;

import java.io.BufferedReader; // New import
import java.io.IOException; // New import
import java.io.InputStream; // New import
import java.io.InputStreamReader; // New import
import java.util.ArrayList;
import java.util.List;
import java.util.Locale; // New import
import java.util.Objects; // New import


public class ManageEmployeesActivity extends AppCompatActivity implements EmployeeAdminAdapter.OnEmployeeActionListener {

    private static final String TAG = "ManageEmployeesActivity";
    private static final int REQUEST_CODE_SELECT_CSV = 102; // Copied from AdminDashboardActivity

    private RecyclerView recyclerEmployeeList;
    private EmployeeAdminAdapter employeeAdminAdapter;
    private DatabaseHelper dbHelper;
    private FloatingActionButton fabAddEmployee;
    private TextView lblNoEmployees;
    private Toolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_employees);

        toolbar = findViewById(R.id.toolbar_manage_employees);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Manage Employees");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_manage_employees), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            // return windowInsets; // Original
            return WindowInsetsCompat.CONSUMED; // Better practice
        });

        dbHelper = new DatabaseHelper(this);

        recyclerEmployeeList = findViewById(R.id.recycler_manage_employee_list);
        lblNoEmployees = findViewById(R.id.lbl_no_employees);
        fabAddEmployee = findViewById(R.id.fab_add_employee);

        recyclerEmployeeList.setLayoutManager(new LinearLayoutManager(this));
        employeeAdminAdapter = new EmployeeAdminAdapter(this, new ArrayList<>(), this);
        recyclerEmployeeList.setAdapter(employeeAdminAdapter);

        fabAddEmployee.setOnClickListener(v -> showAddEmployeeDialog());
    }

    // START: Methods for Toolbar Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.manage_employees_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) { // Handle the back arrow
            finish();
            return true;
        } else if (itemId == R.id.action_import_employees_csv) {
            importEmployeesFromDevice(); // Call the import method
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    // END: Methods for Toolbar Menu


    private void loadEmployeeList() {
        Log.d(TAG, "Loading employee list...");
        List<Employee> employees = dbHelper.getAllEmployees();
        if (employeeAdminAdapter == null) {
            employeeAdminAdapter = new EmployeeAdminAdapter(this, new ArrayList<>(), this);
            recyclerEmployeeList.setAdapter(employeeAdminAdapter);
        }
        employeeAdminAdapter.updateEmployeeList(employees);

        if (employees.isEmpty()) {
            recyclerEmployeeList.setVisibility(View.GONE);
            lblNoEmployees.setVisibility(View.VISIBLE);
        } else {
            recyclerEmployeeList.setVisibility(View.VISIBLE);
            lblNoEmployees.setVisibility(View.GONE);
        }
        Log.d(TAG, (employees.isEmpty() ? "No" : employees.size()) + " employees loaded.");
    }

    @Override
    public void onEditEmployee(Employee employee) {
        showEditEmployeeDialog(employee);
    }

    @Override
    public void onDeleteEmployee(Employee employee) {
        if ("0".equals(employee.getEmployeeNumber())) {
            Toast.makeText(this, "The Guest (0) employee cannot be deleted.", Toast.LENGTH_LONG).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Employee")
                .setMessage("Are you sure you want to delete " + employee.getFullName().trim() + " (" + employee.getEmployeeNumber() + ")?\n\nThis action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    boolean success = dbHelper.deleteEmployeeByNumber(employee.getEmployeeNumber());
                    if (success) {
                        Toast.makeText(ManageEmployeesActivity.this, "Employee " + employee.getFullName().trim() + " deleted.", Toast.LENGTH_SHORT).show();
                        loadEmployeeList();
                    } else {
                        Toast.makeText(ManageEmployeesActivity.this, "Failed to delete employee.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void showAddEmployeeDialog() {
        // ... (your existing showAddEmployeeDialog method - no changes needed here)
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_employee, null);
        builder.setView(dialogView);

        final TextInputEditText dialogEditEmployeeNumber = dialogView.findViewById(R.id.dialog_edit_add_employee_number);
        final TextInputEditText dialogEditFirstName = dialogView.findViewById(R.id.dialog_edit_add_first_name);
        final TextInputEditText dialogEditLastName = dialogView.findViewById(R.id.dialog_edit_add_last_name);
        final TextInputEditText dialogEditDepartment = dialogView.findViewById(R.id.dialog_edit_add_department);

        builder.setTitle("Add New Employee");
        builder.setPositiveButton("Add", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                String employeeNumber = Objects.requireNonNull(dialogEditEmployeeNumber.getText()).toString().trim();
                String firstName = Objects.requireNonNull(dialogEditFirstName.getText()).toString().trim();
                String lastName = Objects.requireNonNull(dialogEditLastName.getText()).toString().trim();
                String department = Objects.requireNonNull(dialogEditDepartment.getText()).toString().trim();

                dialogEditEmployeeNumber.setError(null);
                dialogEditFirstName.setError(null);
                dialogEditLastName.setError(null);

                boolean isValid = true;
                if (employeeNumber.isEmpty()) {
                    dialogEditEmployeeNumber.setError("Number required");
                    isValid = false;
                }
                if (firstName.isEmpty()) {
                    dialogEditFirstName.setError("First name required");
                    isValid = false;
                }
                if (lastName.isEmpty()) {
                    dialogEditLastName.setError("Last name required");
                    isValid = false;
                }

                if (!isValid) {
                    return;
                }

                if (department.isEmpty()) {
                    department = "Unassigned";
                }

                if (dbHelper.isEmployeeNumberTaken(employeeNumber)) {
                    dialogEditEmployeeNumber.setError("Number " + employeeNumber + " already exists.");
                    return;
                }

                long result = dbHelper.insertEmployee(employeeNumber, firstName, lastName, department);
                if (result != -1) {
                    Toast.makeText(ManageEmployeesActivity.this, "Employee " + firstName + " added.", Toast.LENGTH_SHORT).show();
                    loadEmployeeList();
                    alertDialog.dismiss();
                } else {
                    Toast.makeText(ManageEmployeesActivity.this, "Error adding employee.", Toast.LENGTH_SHORT).show();
                }
            });
        });
        alertDialog.show();
    }

    private void showEditEmployeeDialog(final Employee employeeToEdit) {
        // ... (your existing showEditEmployeeDialog method - no changes needed here)
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_employee, null);
        builder.setView(dialogView);

        final TextView dialogTxtEmployeeNumber = dialogView.findViewById(R.id.dialog_txt_employee_number);
        final TextInputEditText dialogEditFirstName = dialogView.findViewById(R.id.dialog_edit_first_name);
        final TextInputEditText dialogEditLastName = dialogView.findViewById(R.id.dialog_edit_last_name);
        final TextInputEditText dialogEditDepartment = dialogView.findViewById(R.id.dialog_edit_department);
        final SwitchMaterial dialogSwitchIsActive = dialogView.findViewById(R.id.dialog_switch_is_active);

        dialogTxtEmployeeNumber.setText(employeeToEdit.getEmployeeNumber());
        dialogEditFirstName.setText(employeeToEdit.getFirstName());
        dialogEditLastName.setText(employeeToEdit.getLastName());
        dialogEditDepartment.setText(employeeToEdit.getDepartment());
        dialogSwitchIsActive.setChecked(employeeToEdit.isActive());

        // This is the NEW code block
        if ("0".equals(employeeToEdit.getEmployeeNumber())) {
            // For the Guest user, lock the text fields but allow the active toggle to be changed
            dialogEditFirstName.setEnabled(false);
            dialogEditLastName.setEnabled(false);
            dialogEditDepartment.setEnabled(false);
            dialogSwitchIsActive.setEnabled(true); // Allow the toggle to be used
            builder.setTitle("Edit Guest Account Status"); // Set a more accurate title
        } else {
            builder.setTitle("Edit Employee: " + employeeToEdit.getEmployeeNumber());
        }

        builder.setPositiveButton("Save", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if ("0".equals(employeeToEdit.getEmployeeNumber())) {
                positiveButton.setVisibility(View.GONE);
            }

            positiveButton.setOnClickListener(view -> {
                String firstName = Objects.requireNonNull(dialogEditFirstName.getText()).toString().trim();
                String lastName = Objects.requireNonNull(dialogEditLastName.getText()).toString().trim();
                String department = Objects.requireNonNull(dialogEditDepartment.getText()).toString().trim();
                boolean isActive = dialogSwitchIsActive.isChecked();

                dialogEditFirstName.setError(null);
                dialogEditLastName.setError(null);

                boolean isValid = true;
                if (TextUtils.isEmpty(firstName)) {
                    dialogEditFirstName.setError("First name cannot be empty.");
                    isValid = false;
                }
                if (TextUtils.isEmpty(lastName)) {
                    dialogEditLastName.setError("Last name cannot be empty.");
                    isValid = false;
                }

                if (!isValid) {
                    return;
                }

                employeeToEdit.setFirstName(firstName);
                employeeToEdit.setLastName(lastName);
                employeeToEdit.setDepartment(department.isEmpty() ? "Unassigned" : department);
                employeeToEdit.setActive(isActive);

                boolean success = dbHelper.updateEmployee(employeeToEdit);
                if (success) {
                    Toast.makeText(ManageEmployeesActivity.this, "Employee updated.", Toast.LENGTH_SHORT).show();
                    loadEmployeeList();
                    alertDialog.dismiss();
                } else {
                    Toast.makeText(ManageEmployeesActivity.this, "Failed to update employee.", Toast.LENGTH_SHORT).show();
                }
            });
        });
        alertDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEmployeeList();
    }

    // START: Copied CSV Import Logic from AdminDashboardActivity
    private void importEmployeesFromDevice() {
        String csvFormatInfo = "Please select a CSV file with the following columns in order:<br>" +
                "1. <b>EmployeeNumber</b> (e.g., 101)<br>" +
                "2. <b>FirstName</b> (e.g., John)<br>" +
                "3. <b>LastName</b> (e.g., Doe)<br>" +
                "4. <b>Department</b> (e.g., Kitchen) <i>(Optional, defaults to 'Imported')</i><br><br>" +
                "No header row is expected in the CSV file.";
        new AlertDialog.Builder(this)
                .setTitle("CSV Import Information")
                .setMessage(Html.fromHtml(csvFormatInfo, Html.FROM_HTML_MODE_LEGACY))
                .setPositiveButton("Select File", (dialog, which) -> launchCsvFilePicker())
                .setNegativeButton("Cancel", null).show();
    }

    private void launchCsvFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv"); // More specific, but EXTRA_MIME_TYPES is good for broader compatibility
        String[] mimeTypes = {
                "text/csv",
                "text/comma-separated-values",
                "application/csv", // Common MIME type
                "application/vnd.ms-excel" // Sometimes used for CSVs
        };
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        try {
            startActivityForResult(Intent.createChooser(intent, "Select a CSV file"), REQUEST_CODE_SELECT_CSV);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager app to select files.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_CSV && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri selectedFileUri = data.getData();
                Log.d(TAG, "CSV file selected: " + selectedFileUri.toString());
                Toast.makeText(this, "Processing CSV: " + selectedFileUri.getLastPathSegment(), Toast.LENGTH_SHORT).show();
                processSelectedCsv(selectedFileUri);
            } else {
                Toast.makeText(this, "No file selected or URI is null.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_CODE_SELECT_CSV && resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(this, "CSV file selection cancelled.", Toast.LENGTH_SHORT).show();
        }
    }

    private void processSelectedCsv(Uri csvFileUri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        // Ensure dialog_progress_spinner.xml exists and has a TextView with R.id.text_progress_message
        View dialogView = inflater.inflate(R.layout.dialog_progress_spinner, null);
        TextView progressText = dialogView.findViewById(R.id.text_progress_message);
        if(progressText != null) progressText.setText("Processing CSV...");
        builder.setView(dialogView);
        builder.setCancelable(false);
        AlertDialog progressDialog = builder.create();
        progressDialog.show();

        new Thread(() -> {
            List<EmployeeFromCsv> employeesToImport;
            final StringBuilder resultMessageBuilder = new StringBuilder(); // Use StringBuilder for messages
            try {
                employeesToImport = readEmployeesFromCsvUri(csvFileUri);
                if (employeesToImport.isEmpty()) {
                    resultMessageBuilder.append("Selected CSV is empty or has no valid data.");
                } else {
                    int newCount = 0, skippedCount = 0, errorCount = 0;
                    for (EmployeeFromCsv empData : employeesToImport) {
                        if (dbHelper.isEmployeeNumberTaken(empData.employeeNumber)) {
                            skippedCount++;
                        } else {
                            if (dbHelper.insertEmployee(empData.employeeNumber, empData.firstName, empData.lastName, empData.department) > 0) {
                                newCount++;
                            } else {
                                errorCount++;
                            }
                        }
                    }
                    resultMessageBuilder.append(String.format(Locale.getDefault(), "CSV Import Complete:\nNew: %d\nSkipped (already exist): %d\nErrors: %d", newCount, skippedCount, errorCount));
                }
            } catch (IOException e) {
                resultMessageBuilder.append("Error reading selected CSV: ").append(e.getMessage());
                Log.e(TAG, "Error reading CSV URI", e);
            }

            runOnUiThread(() -> {
                progressDialog.dismiss();
                // Display result in a more persistent way if needed, or longer Toast
                Toast.makeText(ManageEmployeesActivity.this, resultMessageBuilder.toString(), Toast.LENGTH_LONG).show();
                loadEmployeeList(); // Refresh the employee list on screen
            });
        }).start();
    }

    private List<EmployeeFromCsv> readEmployeesFromCsvUri(Uri csvFileUri) throws IOException {
        List<EmployeeFromCsv> employees = new ArrayList<>();
        // try-with-resources to ensure streams are closed
        try (InputStream inputStream = getContentResolver().openInputStream(csvFileUri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue; // Skip empty lines
                String[] tokens = line.split(","); // Basic CSV split
                if (tokens.length >= 3) { // Need at least EmpNo, FirstName, LastName
                    String empNo = tokens[0].trim();
                    String fName = tokens[1].trim();
                    String lName = tokens[2].trim();
                    String dept = (tokens.length >= 4 && !tokens[3].trim().isEmpty()) ? tokens[3].trim() : "Imported"; // Default department

                    if (!empNo.isEmpty() && !fName.isEmpty() && !lName.isEmpty()) {
                        employees.add(new EmployeeFromCsv(empNo, fName, lName, dept));
                    } else {
                        Log.w(TAG, "Skipping CSV line (URI) with missing essential data: " + line);
                    }
                } else {
                    Log.w(TAG, "Skipping malformed CSV line (URI) - not enough columns: " + line);
                }
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "Failed to open input stream from URI, it might be null.", e);
            throw new IOException("Failed to open input stream from URI.", e);
        }
        return employees;
    }

    // Inner class for holding data from CSV, copied from AdminDashboardActivity
    private static class EmployeeFromCsv {
        String employeeNumber, firstName, lastName, department;
        public EmployeeFromCsv(String en, String fn, String ln, String d) {
            employeeNumber=en; firstName=fn; lastName=ln; department=d;
        }
    }
    // END: Copied CSV Import Logic
}