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
import com.jarindimick.handwashtracking.gui.Employee; // CHANGED IMPORT HERE
import java.util.List;
import java.util.Locale;
import java.util.ArrayList; // Added for initializing list if needed

public class EmployeeAdminAdapter extends RecyclerView.Adapter<EmployeeAdminAdapter.EmployeeViewHolder> {

    private List<Employee> employeeList;
    private Context context;
    private OnEmployeeEditListener editListener;

    public interface OnEmployeeEditListener {
        void onEditEmployee(Employee employee);
    }

    public EmployeeAdminAdapter(Context context, List<Employee> employeeList, OnEmployeeEditListener listener) {
        this.context = context;
        this.employeeList = new ArrayList<>(employeeList); // Create a new list to avoid modifying the original
        this.editListener = listener;
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
            holder.txtStatus.setTextColor(ContextCompat.getColor(context, R.color.teal_700)); // Or a green color
        } else {
            holder.txtStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
        }

        holder.btnEdit.setOnClickListener(v -> {
            if (editListener != null) {
                editListener.onEditEmployee(employee);
            }
        });
    }

    @Override
    public int getItemCount() {
        return employeeList.size();
    }

    // Method to update the list of employees in the adapter
    public void updateEmployeeList(List<Employee> newEmployeeList) {
        this.employeeList.clear();
        this.employeeList.addAll(newEmployeeList);
        notifyDataSetChanged(); // Notifies the RecyclerView to refresh
    }


    static class EmployeeViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtDepartment, txtStatus;
        ImageButton btnEdit;

        EmployeeViewHolder(View view) {
            super(view);
            txtName = view.findViewById(R.id.txt_employee_row_name);
            txtDepartment = view.findViewById(R.id.txt_employee_row_department);
            txtStatus = view.findViewById(R.id.txt_employee_row_status);
            btnEdit = view.findViewById(R.id.btn_employee_row_edit);
        }
    }
}
