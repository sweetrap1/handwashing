package com.jarindimick.handwashtracking.gui;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jarindimick.handwashtracking.R;
import com.jarindimick.handwashtracking.databasehelper.DatabaseHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HandwashLogAdapter extends RecyclerView.Adapter<HandwashLogAdapter.HandwashLogViewHolder> {

    private List<DatabaseHelper.HandwashLog> logs;
    private Context context;

    public HandwashLogAdapter(List<DatabaseHelper.HandwashLog> logs, Context context) {
        this.logs = logs;
        this.context = context;
    }

    @NonNull
    @Override
    public HandwashLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_handwash_log, parent, false);
        return new HandwashLogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HandwashLogViewHolder holder, int position) {
        final DatabaseHelper.HandwashLog log = logs.get(position);

        // Create a full name from the first and last name parts
        String fullName = (log.firstName + " " + log.lastName).trim();

// Set the text to show the full name and the number in parentheses
        holder.txt_employee_number.setText(fullName + " (" + log.employeeNumber + ")");

        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        try {
            Date date = inputFormat.parse(log.washDate);
            if (date != null) {
                holder.txt_wash_date.setText("Wash Date: " + outputFormat.format(date));
            } else {
                holder.txt_wash_date.setText("Wash Date: " + log.washDate);
            }
        } catch (ParseException e) {
            holder.txt_wash_date.setText("Wash Date: " + log.washDate);
            Log.e("HandwashLogAdapter", "Error parsing date: " + log.washDate, e);
        }

        holder.txt_wash_time.setText("Wash Time: " + log.washTime);

        // Display the confirmation status from the photoPath field
        String status = (log.photoPath != null && !log.photoPath.isEmpty()) ? log.photoPath : "N/A";
        holder.txt_confirmation_status.setText("Status: " + status);
        // Make the text non-clickable
        holder.txt_confirmation_status.setClickable(false);
        holder.txt_confirmation_status.setFocusable(false);
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    public static class HandwashLogViewHolder extends RecyclerView.ViewHolder {
        TextView txt_employee_number;
        TextView txt_wash_date;
        TextView txt_wash_time;
        TextView txt_confirmation_status; // Changed from txt_photo_path

        public HandwashLogViewHolder(@NonNull View itemView) {
            super(itemView);
            txt_employee_number = itemView.findViewById(R.id.txt_employee_number);
            txt_wash_date = itemView.findViewById(R.id.txt_wash_date);
            txt_wash_time = itemView.findViewById(R.id.txt_wash_time);
            txt_confirmation_status = itemView.findViewById(R.id.txt_photo_path); // Keep the ID, but we'll use it for status
        }
    }
}