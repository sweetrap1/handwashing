package com.jarindimick.handwashtracking.gui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.jarindimick.handwashtracking.R;
import com.jarindimick.handwashtracking.databasehelper.DatabaseHelper.ComplianceResult;
import java.util.List;
import java.util.Locale;

public class ComplianceReportAdapter extends RecyclerView.Adapter<ComplianceReportAdapter.ViewHolder> {

    private final List<ComplianceResult> reportData;

    // This constructor was likely missing from your file, causing the error
    public ComplianceReportAdapter(List<ComplianceResult> reportData) {
        this.reportData = reportData;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_compliance_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ComplianceResult result = reportData.get(position);
        holder.txtEmployeeName.setText(String.format(Locale.getDefault(), "%s (%s)",
                result.employeeName.trim(), result.employeeNumber));

        // Build the string for the hourly status
        StringBuilder statusBuilder = new StringBuilder();
        if (result.hourlyStatus.isEmpty()) {
            statusBuilder.append("No washes logged during shift hours.");
        } else {
            for (java.util.Map.Entry<Integer, Boolean> entry : result.hourlyStatus.entrySet()) {
                int hour = entry.getKey();
                boolean isCompliant = entry.getValue();

                // Format hour to 12-hour AM/PM format
                String amPm = (hour < 12 || hour == 24) ? "am" : "pm";
                int displayHour = (hour == 0 || hour == 12) ? 12 : hour % 12;

                statusBuilder.append(String.format(Locale.getDefault(), "%d%s: %s   ",
                        displayHour, amPm, (isCompliant ? "✅" : "❌")));
            }
        }
        holder.txtHourlyStatus.setText(statusBuilder.toString().trim());
    }

    // ADD THIS METHOD to ComplianceReportAdapter.java
    public List<ComplianceResult> getReportData() {
        return reportData;
    }

    @Override
    public int getItemCount() {
        return reportData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView txtEmployeeName;
        final TextView txtHourlyStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtEmployeeName = itemView.findViewById(R.id.txt_employee_name_report);
            txtHourlyStatus = itemView.findViewById(R.id.txt_hourly_status_report);
        }
    }
}