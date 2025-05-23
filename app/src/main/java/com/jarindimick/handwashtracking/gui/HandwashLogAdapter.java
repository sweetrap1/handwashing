package com.jarindimick.handwashtracking.gui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log; // Import Log
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.jarindimick.handwashtracking.R;
import com.jarindimick.handwashtracking.databasehelper.DatabaseHelper;

import java.io.File;
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
        DatabaseHelper.HandwashLog log = logs.get(position);

        // --- ADD LOGGING HERE TO INSPECT DATA ---
        Log.d("HandwashLogAdapter", "Log " + position + ":");
        Log.d("HandwashLogAdapter", "  Employee Number: " + log.employeeNumber);
        Log.d("HandwashLogAdapter", "  Wash Date: " + log.washDate);
        Log.d("HandwashLogAdapter", "  Wash Time: " + log.washTime);
        Log.d("HandwashLogAdapter", "  Photo Path: " + log.photoPath);
        // --- END LOGGING ---


        holder.txt_employee_number.setText("Employee Number: " + log.employeeNumber);

        // Format the date
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM dd,yyyy", Locale.getDefault());
        try {
            Date date = inputFormat.parse(log.washDate);
            if (date != null) {
                holder.txt_wash_date.setText("Wash Date: " + outputFormat.format(date));
            } else {
                holder.txt_wash_date.setText("Wash Date: " + log.washDate); // Original if parsing fails
            }
        } catch (ParseException e) {
            holder.txt_wash_date.setText("Wash Date: " + log.washDate); // Original if parsing fails
            Log.e("HandwashLogAdapter", "Error parsing date: " + log.washDate, e); // Log the error
        }

        holder.txt_wash_time.setText("Wash Time: " + log.washTime);
        holder.txt_photo_path.setText("Photo Path: " + (log.photoPath.isEmpty() ? "N/A" : "View Photo"));

        holder.txt_photo_path.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!log.photoPath.isEmpty()) {
                    File photoFile = new File(log.photoPath);
                    if (photoFile.exists()) {
                        try {
                            Uri photoUri = FileProvider.getUriForFile(context,
                                    "com.jarindimick.handwashtracking.fileprovider",
                                    photoFile);

                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(photoUri, "image/*");
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            if (intent.resolveActivity(context.getPackageManager()) != null) {
                                context.startActivity(intent);
                            } else {
                                Toast.makeText(context, "No app found to view this photo.", Toast.LENGTH_SHORT).show();
                            }
                        } catch (IllegalArgumentException e) {
                            Toast.makeText(context, "Error: Could not open photo (FileProvider issue).", Toast.LENGTH_SHORT).show();
                            Log.e("HandwashLogAdapter", "FileProvider error: " + e.getMessage(), e);
                        } catch (Exception e) {
                            Toast.makeText(context, "Error opening photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e("HandwashLogAdapter", "General error opening photo: " + e.getMessage(), e);
                        }
                    } else {
                        Toast.makeText(context, "Photo file not found on device.", Toast.LENGTH_SHORT).show();
                        Log.w("HandwashLogAdapter", "Photo file path exists in DB but file not found on disk: " + log.photoPath);
                    }
                } else {
                    Toast.makeText(context, "No photo available for this log.", Toast.LENGTH_SHORT).show();
                }
            }
        });
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