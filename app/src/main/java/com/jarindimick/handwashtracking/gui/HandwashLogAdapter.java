package com.jarindimick.handwashtracking.gui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView; // Was androidx.core.content.FileProvider; - this import is not needed here

import com.jarindimick.handwashtracking.R;
import com.jarindimick.handwashtracking.databasehelper.DatabaseHelper;

// import java.io.File; // No longer strictly needed here for this functionality
// import java.io.FileNotFoundException; // Only if you add explicit content resolver checks
// import java.io.InputStream; // Only if you add explicit content resolver checks
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

        Log.d("HandwashLogAdapter", "Binding log for Emp: " + log.employeeNumber + ", PhotoPath: " + log.photoPath);

        holder.txt_employee_number.setText("Employee Number: " + log.employeeNumber);

        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()); // Corrected year pattern
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

        // Determine if a photo path/URI exists and is not empty or explicitly "null" (as a string)
        boolean hasPhotoIdentifier = log.photoPath != null && !log.photoPath.isEmpty() && !log.photoPath.equalsIgnoreCase("null");

        holder.txt_photo_path.setText("Photo: " + (hasPhotoIdentifier ? "View Photo" : "N/A"));
        holder.txt_photo_path.setClickable(hasPhotoIdentifier);
        holder.txt_photo_path.setFocusable(hasPhotoIdentifier);


        if (hasPhotoIdentifier) {
            holder.txt_photo_path.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Uri photoUriToView = null;
                    try {
                        // The stored log.photoPath is expected to be a URI string
                        photoUriToView = Uri.parse(log.photoPath);
                    } catch (Exception e) {
                        Log.e("HandwashLogAdapter", "Error parsing stored photo path as URI: " + log.photoPath, e);
                        Toast.makeText(context, "Invalid photo reference in database.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (photoUriToView != null) {
                        Log.d("HandwashLogAdapter", "Attempting to view photo with URI: " + photoUriToView.toString());
                        try {
                            // Optional: A quick check to see if content resolver can open it (catches some bad URIs)
                            // This check can be useful for debugging but might be too strict if some apps can handle URIs
                            // that openInputStream might fail for (e.g. due to temporary permissions needed by the viewer app)
                            //
                            // InputStream inputStream = context.getContentResolver().openInputStream(photoUriToView);
                            // if (inputStream != null) {
                            //     inputStream.close();
                            // } else {
                            //     throw new FileNotFoundException("Content resolver returned null stream for URI.");
                            // }

                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(photoUriToView, "image/*");
                            // Grant permission to the receiving app to read this URI
                            // This is crucial for FileProvider URIs and good practice for MediaStore URIs too.
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            if (intent.resolveActivity(context.getPackageManager()) != null) {
                                context.startActivity(intent);
                            } else {
                                Toast.makeText(context, "No app found to view this photo.", Toast.LENGTH_SHORT).show();
                                Log.w("HandwashLogAdapter", "No activity found to handle ACTION_VIEW for image URI: " + photoUriToView.toString());
                            }
                        } catch (SecurityException se) {
                            Toast.makeText(context, "Permission denied: Cannot open photo.", Toast.LENGTH_LONG).show();
                            Log.e("HandwashLogAdapter", "SecurityException opening photo URI: " + photoUriToView.toString(), se);
                        } catch (Exception e) { // Catch other exceptions like ActivityNotFoundException, FileNotFoundException from optional check
                            Toast.makeText(context, "Could not open photo: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e("HandwashLogAdapter", "Error opening photo URI: " + photoUriToView.toString(), e);
                        }
                    } else {
                        // This case might be reached if log.photoPath was an empty string that somehow passed the initial check
                        // or if Uri.parse returns null for some malformed strings (though it usually throws an exception).
                        Toast.makeText(context, "Photo reference is invalid (null after parse).", Toast.LENGTH_LONG).show();
                    }
                }
            });
        } else {
            // If no photo identifier, ensure no listener is set (or remove if set previously)
            holder.txt_photo_path.setOnClickListener(null);
        }
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