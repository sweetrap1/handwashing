package com.jarindimick.handwashtracking.gui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.jarindimick.handwashtracking.R;
// import com.jarindimick.handwashtracking.gui.Employee; // This line can be removed if Employee class is in the same package, or ensure it's correct.
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;

public class EmployeeAdminAdapter extends RecyclerView.Adapter<EmployeeAdminAdapter.EmployeeViewHolder> {

    private List<Employee> employeeList;
    private Context context;
    private OnEmployeeActionListener actionListener; // Changed listener name for clarity

    // Updated interface to include delete action
    public interface OnEmployeeActionListener {
        void onEditEmployee(Employee employee);
        void onDeleteEmployee(Employee employee); // New method for delete
    }

    public EmployeeAdminAdapter(Context context, List<Employee> employeeList, OnEmployeeActionListener listener) {
        this.context = context;
        this.employeeList = new ArrayList<>(employeeList);
        this.actionListener = listener; // Updated listener
    }

    @NonNull
    @Override
    public EmployeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_employee_admin, parent, false);
        return new EmployeeViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull EmployeeViewHolder holder, int position) {
        Employee employee = employeeList.get(position);

        holder.txtName.setText(String.format(Locale.getDefault(), "%s (%s)",
                employee.getFullName().trim(), employee.getEmployeeNumber()));
        holder.txtDepartment.setText(String.format("Department: %s",
                employee.getDepartment() == null || employee.getDepartment().isEmpty() ? "N/A" : employee.getDepartment()));
        holder.txtStatus.setText(String.format("Status: %s", employee.isActive() ? "Active" : "Inactive"));

        if (employee.isActive()) {
            holder.txtStatus.setTextColor(ContextCompat.getColor(context, R.color.teal_700));
        } else {
            holder.txtStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
        }

        holder.btnEdit.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onEditEmployee(employee);
            }
        });

        // START: Handle Delete Button
        holder.btnDelete.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onDeleteEmployee(employee);
            }
        });

        // Hide delete button for the "Guest" user (employee number "0")
        if ("0".equals(employee.getEmployeeNumber())) {
            holder.btnDelete.setVisibility(View.GONE);
        } else {
            holder.btnDelete.setVisibility(View.VISIBLE);
        }
        // END: Handle Delete Button
    }

    @Override
    public int getItemCount() {
        return employeeList.size();
    }

    public void updateEmployeeList(List<Employee> newEmployeeList) {
        this.employeeList.clear();
        this.employeeList.addAll(newEmployeeList);
        notifyDataSetChanged();
    }


    static class EmployeeViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtDepartment, txtStatus;
        ImageButton btnEdit, btnDelete; // Added btnDelete

        EmployeeViewHolder(View view) {
            super(view);
            txtName = view.findViewById(R.id.txt_employee_row_name);
            txtDepartment = view.findViewById(R.id.txt_employee_row_department);
            txtStatus = view.findViewById(R.id.txt_employee_row_status);
            btnEdit = view.findViewById(R.id.btn_employee_row_edit);
            btnDelete = view.findViewById(R.id.btn_employee_row_delete); // Initialize btnDelete
        }
    }
}