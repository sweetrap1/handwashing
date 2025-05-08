package com.jarindimick.handwashtracking.gui;

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

    public HandwashLogAdapter(List<DatabaseHelper.HandwashLog> logs) {
        this.logs = logs;
    }

    @NonNull
    @Override
    public HandwashLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_handwash_log, parent, false);
        return new HandwashLogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HandwashLogViewHolder holder, int position) {
        DatabaseHelper.HandwashLog log = logs.get(position);
        holder.txt_employee_number.setText("Employee Number: " + log.employeeNumber);

        // Format the date
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        try {
            Date date = inputFormat.parse(log.washDate);
            if (date != null) {
                holder.txt_wash_date.setText("Wash Date: " + outputFormat.format(date));
            } else {
                holder.txt_wash_date.setText("Wash Date: " + log.washDate); // Original if parsing fails
            }
        } catch (ParseException e) {
            holder.txt_wash_date.setText("Wash Date: " + log.washDate); // Original if parsing fails
            e.printStackTrace(); // Log the error
        }

        holder.txt_wash_time.setText("Wash Time: " + log.washTime);
        holder.txt_photo_path.setText("Photo Path: " + log.photoPath);
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    public static class HandwashLogViewHolder extends RecyclerView.ViewHolder {
        TextView txt_employee_number;
        TextView txt_wash_date;
        TextView txt_wash_time;
        TextView txt_photo_path;

        public HandwashLogViewHolder(@NonNull View itemView) {
            super(itemView);
            txt_employee_number = itemView.findViewById(R.id.txt_employee_number);
            txt_wash_date = itemView.findViewById(R.id.txt_wash_date);
            txt_wash_time = itemView.findViewById(R.id.txt_wash_time);
            txt_photo_path = itemView.findViewById(R.id.txt_photo_path);
        }
    }
}