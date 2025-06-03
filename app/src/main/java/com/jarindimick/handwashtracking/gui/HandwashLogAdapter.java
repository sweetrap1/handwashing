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
import androidx.core.content.FileProvider; // Needed for converting file paths to content URIs
import androidx.recyclerview.widget.RecyclerView;

import com.jarindimick.handwashtracking.R;
import com.jarindimick.handwashtracking.databasehelper.DatabaseHelper;

import java.io.File; // Needed for creating File objects from paths
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
        final DatabaseHelper.HandwashLog log = logs.get(position); // Made log final for use in inner class

        // Log.d("HandwashLogAdapter", "Binding log for Emp: " + log.employeeNumber + ", PhotoPath: " + log.photoPath); // This log is good for initial binding check

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

        boolean hasPhotoIdentifier = log.photoPath != null && !log.photoPath.isEmpty() && !log.photoPath.equalsIgnoreCase("null");

        holder.txt_photo_path.setText("Photo: " + (hasPhotoIdentifier ? "View Photo" : "N/A"));
        holder.txt_photo_path.setClickable(hasPhotoIdentifier);
        holder.txt_photo_path.setFocusable(hasPhotoIdentifier);

        if (hasPhotoIdentifier) {
            holder.txt_photo_path.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String pathFromDb = log.photoPath;

                    if (pathFromDb == null || pathFromDb.isEmpty() || pathFromDb.equalsIgnoreCase("null")) {
                        Toast.makeText(context, "Photo reference is missing.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    Log.d("HandwashLogAdapter", "Raw photoPath from DB: " + pathFromDb);
                    Uri photoUriToView;

                    try {
                        Uri parsedUri = Uri.parse(pathFromDb);
                        String scheme = parsedUri.getScheme();
                        Log.d("HandwashLogAdapter", "Parsed URI Scheme: " + (scheme != null ? scheme : "null"));

                        if ("file".equals(scheme)) {
                            Log.d("HandwashLogAdapter", "Path from DB is a file scheme. Attempting FileProvider conversion.");
                            File photoFile = new File(parsedUri.getPath()); // Get path from file URI
                            if (photoFile.exists()) {
                                // Authority must match exactly what's in AndroidManifest.xml
                                String authority = context.getApplicationContext().getPackageName() + ".fileprovider";
                                photoUriToView = FileProvider.getUriForFile(context, authority, photoFile);
                                Log.d("HandwashLogAdapter", "Converted to FileProvider URI: " + photoUriToView.toString());
                            } else {
                                Log.e("HandwashLogAdapter", "File URI points to a non-existent file: " + parsedUri.getPath());
                                Toast.makeText(context, "Photo file not found at path: " + parsedUri.getPath(), Toast.LENGTH_LONG).show();
                                return;
                            }
                        } else if ("content".equals(scheme)) {
                            Log.d("HandwashLogAdapter", "Path from DB is already a content scheme URI: " + parsedUri.toString());
                            photoUriToView = parsedUri;
                        } else {
                            // Attempt to treat as a file path if no scheme, though this is less reliable
                            Log.w("HandwashLogAdapter", "URI scheme is null or unknown ('" + scheme + "'). Assuming it might be a raw file path and attempting FileProvider conversion.");
                            File photoFile = new File(pathFromDb);
                            if (photoFile.exists()) {
                                String authority = context.getApplicationContext().getPackageName() + ".fileprovider";
                                photoUriToView = FileProvider.getUriForFile(context, authority, photoFile);
                                Log.d("HandwashLogAdapter", "Converted assumed file path to FileProvider URI: " + photoUriToView.toString());
                            } else {
                                Log.e("HandwashLogAdapter", "Assumed file path does not exist: " + pathFromDb);
                                Toast.makeText(context, "Photo file not found for path: " + pathFromDb, Toast.LENGTH_LONG).show();
                                return;
                            }
                        }
                    } catch (Exception e) {
                        Log.e("HandwashLogAdapter", "Error processing photo path: '" + pathFromDb + "' into a usable URI.", e);
                        Toast.makeText(context, "Invalid photo reference format.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (photoUriToView == null) {
                        Toast.makeText(context, "Could not obtain a valid URI for the photo.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    Log.d("HandwashLogAdapter", "Attempting to view with final URI: " + photoUriToView.toString() + " (Scheme: " + photoUriToView.getScheme() + ")");

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(photoUriToView, "image/*"); // Standard way to set data and type
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Often helps when starting external activities

                    if (intent.resolveActivity(context.getPackageManager()) != null) {
                        try {
                            context.startActivity(intent);
                        } catch (SecurityException se) {
                            Log.e("HandwashLogAdapter", "SecurityException opening photo URI: " + photoUriToView.toString(), se);
                            Toast.makeText(context, "Permission denied: Cannot open photo.", Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Log.e("HandwashLogAdapter", "Error starting activity for URI: " + photoUriToView.toString(), e);
                            Toast.makeText(context, "Could not open photo: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(context, "No app found to view this photo.", Toast.LENGTH_SHORT).show();
                        Log.w("HandwashLogAdapter", "No activity found for ACTION_VIEW with URI: " + photoUriToView.toString());

                        // Fallback attempt with chooser for further debugging
                        Intent chooserIntent = Intent.createChooser(intent, "Open image with");
                        if (chooserIntent.resolveActivity(context.getPackageManager()) != null) {
                            Log.d("HandwashLogAdapter", "Chooser has options, trying to start chooser...");
                            try {
                                context.startActivity(chooserIntent);
                            } catch (Exception e) {
                                Log.e("HandwashLogAdapter", "Error starting chooser: " + e.getMessage());
                            }
                        } else {
                            Log.w("HandwashLogAdapter", "Chooser also found no activities for URI: " + photoUriToView.toString());
                        }
                    }
                }
            });
        } else {
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