package com.jarindimick.handwashtracking.gui;

import android.content.DialogInterface;
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

public class ManageEmployeesActivity extends AppCompatActivity implements EmployeeAdminAdapter.OnEmployeeEditListener {

    private static final String TAG = "ManageEmployeesActivity";

    private RecyclerView recyclerEmployeeList;
    private EmployeeAdminAdapter employeeAdminAdapter;
    private DatabaseHelper dbHelper;
    private FloatingActionButton fabAddEmployee;
    private TextView lblNoEmployees;
    private Toolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // IMPORTANT: Call EdgeToEdge.enable before super.onCreate() or at least before setContentView()
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

        // Apply insets for edge-to-edge display
        // This listener will be called when the window insets change (e.g., keyboard shown/hidden, system bars)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_manage_employees), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Apply the system bar insets as padding to the root view (v)
            // This will push the content down from the status bar and up from the navigation bar.
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);

            // It's good practice to return the insets parameter unless you've fully consumed them
            // and don't want child views to receive them.
            return windowInsets;
        });

        dbHelper = new DatabaseHelper(this);

        recyclerEmployeeList = findViewById(R.id.recycler_manage_employee_list);
        lblNoEmployees = findViewById(R.id.lbl_no_employees);
        fabAddEmployee = findViewById(R.id.fab_add_employee);

        recyclerEmployeeList.setLayoutManager(new LinearLayoutManager(this));
        employeeAdminAdapter = new EmployeeAdminAdapter(this, new ArrayList<>(), this);
        recyclerEmployeeList.setAdapter(employeeAdminAdapter);

        fabAddEmployee.setOnClickListener(v -> showAddEmployeeDialog());

        // loadEmployeeList(); // Moved to onResume to refresh data if coming back to activity
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Close this activity and return to previous one (AdminDashboardActivity)
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void loadEmployeeList() {
        Log.d(TAG, "Loading employee list...");
        List<Employee> employees = dbHelper.getAllEmployees(); // Make sure dbHelper is initialized
        if (employeeAdminAdapter == null) { // Should not happen if onCreate is correctly setup
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

                if (employeeNumber.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
                    Toast.makeText(ManageEmployeesActivity.this, "Employee Number, First & Last Name required.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (department.isEmpty()) {
                    department = "Unassigned";
                }
                if (dbHelper.isEmployeeNumberTaken(employeeNumber)) {
                    Toast.makeText(ManageEmployeesActivity.this, "Employee number " + employeeNumber + " already exists.", Toast.LENGTH_SHORT).show();
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

        builder.setTitle("Edit Employee: " + employeeToEdit.getEmployeeNumber());
        builder.setPositiveButton("Save", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                String firstName = Objects.requireNonNull(dialogEditFirstName.getText()).toString().trim();
                String lastName = Objects.requireNonNull(dialogEditLastName.getText()).toString().trim();
                String department = Objects.requireNonNull(dialogEditDepartment.getText()).toString().trim();
                boolean isActive = dialogSwitchIsActive.isChecked();

                if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName)) {
                    Toast.makeText(ManageEmployeesActivity.this, "First and Last name cannot be empty.", Toast.LENGTH_SHORT).show();
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
        loadEmployeeList(); // Refresh list when activity resumes
    }
}