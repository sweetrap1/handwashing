package com.jarindimick.handwashtracking.gui;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Changed to implement the new interface
public class ManageEmployeesActivity extends AppCompatActivity implements EmployeeAdminAdapter.OnEmployeeActionListener {

    private static final String TAG = "ManageEmployeesActivity";

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
            return windowInsets;
        });

        dbHelper = new DatabaseHelper(this);

        recyclerEmployeeList = findViewById(R.id.recycler_manage_employee_list);
        lblNoEmployees = findViewById(R.id.lbl_no_employees);
        fabAddEmployee = findViewById(R.id.fab_add_employee);

        recyclerEmployeeList.setLayoutManager(new LinearLayoutManager(this));
        // Pass 'this' as the OnEmployeeActionListener
        employeeAdminAdapter = new EmployeeAdminAdapter(this, new ArrayList<>(), this);
        recyclerEmployeeList.setAdapter(employeeAdminAdapter);

        fabAddEmployee.setOnClickListener(v -> showAddEmployeeDialog());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void loadEmployeeList() {
        Log.d(TAG, "Loading employee list...");
        List<Employee> employees = dbHelper.getAllEmployees();
        if (employeeAdminAdapter == null) {
            // Pass 'this' as the OnEmployeeActionListener
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

    // START: Implemented onDeleteEmployee
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
                        loadEmployeeList(); // Refresh the list
                    } else {
                        Toast.makeText(ManageEmployeesActivity.this, "Failed to delete employee.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert) // Adds a warning icon
                .show();
    }
    // END: Implemented onDeleteEmployee

    private void showAddEmployeeDialog() {
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

        // Disable editing for Guest "0" user
        if ("0".equals(employeeToEdit.getEmployeeNumber())) {
            dialogEditFirstName.setEnabled(false);
            dialogEditLastName.setEnabled(false);
            dialogEditDepartment.setEnabled(false);
            dialogSwitchIsActive.setEnabled(false); // Guest should always be active
            builder.setTitle("View Guest Employee (Cannot Edit)");
        } else {
            builder.setTitle("Edit Employee: " + employeeToEdit.getEmployeeNumber());
        }


        builder.setPositiveButton("Save", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if ("0".equals(employeeToEdit.getEmployeeNumber())) { // If guest, hide/disable save
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
}