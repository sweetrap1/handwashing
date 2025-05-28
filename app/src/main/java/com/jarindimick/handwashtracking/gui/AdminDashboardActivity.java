package com.jarindimick.handwashtracking.gui;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences; // NEW IMPORT
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap; // NEW IMPORT
import android.graphics.BitmapFactory; // NEW IMPORT
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import com.jarindimick.handwashtracking.R;
import com.jarindimick.handwashtracking.databasehelper.DatabaseHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException; // NEW IMPORT
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class AdminDashboardActivity extends AppCompatActivity {
    private static final String TAG = "AdminDashboardActivity";
    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 101;
    private static final int REQUEST_CODE_SELECT_CSV = 102;
    private static final int REQUEST_CODE_SELECT_LOGO_IMAGE = 103; // NEW: For logo image picker

    // NEW: SharedPreferences and logo constants
    public static final String PREFS_NAME = "HandwashAppPrefs";
    public static final String KEY_CUSTOM_LOGO_PATH = "custom_logo_path";
    public static final String CUSTOM_LOGO_FILENAME = "custom_app_logo.png";


    // UI elements for Overview
    private TextView txt_overview_total_washes_today;
    private TextView txt_overview_active_employees;
    private TextView txt_overview_top_washer_today;

    // UI elements for Search Handwashes
    private EditText edit_search_first_name;
    private EditText edit_search_last_name;
    private EditText edit_search_employee_id;
    private EditText edit_search_start_date;
    private EditText edit_search_end_date;
    private Button btn_search_handwashes;
    private RecyclerView recycler_search_results;
    private HandwashLogAdapter handwashLogAdapter;

    private TextView txt_message;
    private MaterialToolbar toolbarAdminDashboard;

    // UI elements for Action Buttons
    private Button btn_logout;
    private Button btn_delete_data;
    private Button btn_import_employees;
    private Button btn_go_to_manage_employees;
    private Button btn_upload_logo; // NEW: Button for logo upload

    // Buttons for Dialog Triggers
    private Button btn_show_add_employee_dialog;
    private Button btn_show_change_password_dialog;
    private Button btn_show_download_data_dialog;

    private DatabaseHelper dbHelper;

    private String pendingCsvData;
    private String pendingFileName;
    private AlertDialog pendingDialogToDismiss;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        toolbarAdminDashboard = findViewById(R.id.toolbar_admin_dashboard);
        setSupportActionBar(toolbarAdminDashboard);

        CoordinatorLayout coordinatorLayout = findViewById(R.id.coordinator_layout_admin_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(coordinatorLayout, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, 0, insets.right, insets.bottom);
            return windowInsets;
        });

        dbHelper = new DatabaseHelper(this);
        setupgui();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAdminOverviewData();
    }

    private void setupgui() {
        txt_overview_total_washes_today = findViewById(R.id.txt_overview_total_washes_today);
        txt_overview_active_employees = findViewById(R.id.txt_overview_active_employees);
        txt_overview_top_washer_today = findViewById(R.id.txt_overview_top_washer_today);

        edit_search_first_name = findViewById(R.id.edit_search_first_name);
        edit_search_last_name = findViewById(R.id.edit_search_last_name);
        edit_search_employee_id = findViewById(R.id.edit_search_employee_id);
        edit_search_start_date = findViewById(R.id.edit_search_start_date);
        edit_search_end_date = findViewById(R.id.edit_search_end_date);
        btn_search_handwashes = findViewById(R.id.btn_search_handwashes);
        recycler_search_results = findViewById(R.id.recycler_search_results);
        recycler_search_results.setLayoutManager(new LinearLayoutManager(this));

        btn_logout = findViewById(R.id.btn_logout);
        btn_delete_data = findViewById(R.id.btn_delete_data);
        btn_import_employees = findViewById(R.id.btn_import_employees);
        txt_message = findViewById(R.id.txt_message);

        btn_go_to_manage_employees = findViewById(R.id.btn_go_to_manage_employees);
        btn_upload_logo = findViewById(R.id.btn_upload_logo); // Initialize new button

        btn_show_add_employee_dialog = findViewById(R.id.btn_show_add_employee_dialog);
        btn_show_change_password_dialog = findViewById(R.id.btn_show_change_password_dialog);
        btn_show_download_data_dialog = findViewById(R.id.btn_show_download_data_dialog);
    }

    private void setupListeners() {
        edit_search_start_date.setOnClickListener(v -> showDatePickerDialog(edit_search_start_date, "Set Search Start Date"));
        edit_search_end_date.setOnClickListener(v -> showDatePickerDialog(edit_search_end_date, "Set Search End Date"));
        btn_search_handwashes.setOnClickListener(v -> searchHandwashes());

        btn_logout.setOnClickListener(v -> logout());
        btn_delete_data.setOnClickListener(v -> deleteDataWithConfirmation());
        btn_import_employees.setOnClickListener(v -> importEmployeesFromDevice());
        btn_upload_logo.setOnClickListener(v -> openImagePicker()); // Set listener for new button

        btn_go_to_manage_employees.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, ManageEmployeesActivity.class);
            startActivity(intent);
        });

        btn_show_add_employee_dialog.setOnClickListener(v -> showAddEmployeeDialog());
        btn_show_change_password_dialog.setOnClickListener(v -> showChangePasswordDialog());
        btn_show_download_data_dialog.setOnClickListener(v -> showDownloadDataDialog());
    }

    // NEW: Method to open image picker
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT); // Or ACTION_GET_CONTENT
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        try {
            startActivityForResult(intent, REQUEST_CODE_SELECT_LOGO_IMAGE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager or Gallery app.", Toast.LENGTH_SHORT).show();
            txt_message.setText("No app found to select an image.");
        }
    }

    // NEW: Method to save selected logo to internal storage and update SharedPreferences
    private void saveLogoToAppStorage(Uri sourceUri) {
        if (sourceUri == null) {
            Toast.makeText(this, "Failed to get image.", Toast.LENGTH_SHORT).show();
            return;
        }

        File internalDir = getFilesDir(); // App's internal files directory
        File logoFile = new File(internalDir, CUSTOM_LOGO_FILENAME);

        try (InputStream inputStream = getContentResolver().openInputStream(sourceUri);
             OutputStream outputStream = new FileOutputStream(logoFile)) {

            if (inputStream == null) {
                throw new IOException("Unable to open input stream from URI");
            }

            // You might want to resize the bitmap here if it's very large
            // For simplicity, we're copying directly.
            // For production, consider resizing to a reasonable dimension.
            // Example:
            // Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            // Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true); // Example: scale to 200x200
            // scaledBitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_CUSTOM_LOGO_PATH, logoFile.getAbsolutePath());
            editor.apply();

            Toast.makeText(this, "App logo updated successfully!", Toast.LENGTH_SHORT).show();
            txt_message.setText("Logo updated. It will show on the main screen.");
            Log.d(TAG, "Logo saved to: " + logoFile.getAbsolutePath());

        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found for logo saving: " + e.getMessage(), e);
            Toast.makeText(this, "Error: Source image not found.", Toast.LENGTH_LONG).show();
            txt_message.setText("Error: Could not find the selected image.");
        } catch (IOException e) {
            Log.e(TAG, "Error saving logo: " + e.getMessage(), e);
            Toast.makeText(this, "Error saving logo. Please try again.", Toast.LENGTH_LONG).show();
            txt_message.setText("Error saving logo to app storage.");
        }
    }


    // ... (loadAdminOverviewData, showDatePickerDialog, showDownloadDataDialog, performDataDownloadFromDialog, etc. methods remain the same)
    // ... I will include them again for completeness of this file if requested, but the logic inside them doesn't change for this specific feature.
    // ... For brevity, I'll assume they are present from the previous step. If you need the full file with them, let me know.

    // MODIFIED: onActivityResult to handle both CSV and LOGO image selection
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            if (requestCode == REQUEST_CODE_SELECT_CSV) {
                Uri selectedFileUri = data.getData();
                Log.d(TAG, "CSV file selected: " + selectedFileUri.toString());
                txt_message.setText("Processing selected CSV: " + selectedFileUri.getLastPathSegment());
                processSelectedCsv(selectedFileUri);
            } else if (requestCode == REQUEST_CODE_SELECT_LOGO_IMAGE) {
                Uri selectedImageUri = data.getData();
                Log.d(TAG, "Logo image selected: " + selectedImageUri.toString());
                txt_message.setText("Processing selected logo...");
                saveLogoToAppStorage(selectedImageUri);
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Log.d(TAG, "File/Image selection cancelled. Request code: " + requestCode);
            // Optionally show a message if selection was cancelled
            if (requestCode == REQUEST_CODE_SELECT_LOGO_IMAGE) {
                txt_message.setText("Logo selection cancelled.");
            }
        } else {
            Log.w(TAG, "onActivityResult with resultCode: " + resultCode + " and no data or null URI for requestCode: " + requestCode);
            if (requestCode == REQUEST_CODE_SELECT_LOGO_IMAGE) {
                txt_message.setText("Failed to select logo image.");
                Toast.makeText(this, "Failed to get image.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // --- All other methods like loadAdminOverviewData, showDatePickerDialog, download dialogs, search, logout, delete, import CSV logic, password/employee dialogs, onRequestPermissionsResult, EmployeeFromCsv class, onDestroy ---
    // --- should remain as they were in the previous version. ---
    // --- For brevity, I'm not re-pasting all of them here but ensure they are present in your file. ---
    // --- The key additions are openImagePicker(), saveLogoToAppStorage(), and the new case in onActivityResult(). ---


    private void loadAdminOverviewData() {
        Log.d(TAG, "Loading admin overview data...");
        if (dbHelper == null) dbHelper = new DatabaseHelper(this);
        txt_overview_total_washes_today.setText(String.format(Locale.getDefault(), "Total Handwashes Today: %d", dbHelper.getTotalHandwashesToday()));
        txt_overview_active_employees.setText(String.format(Locale.getDefault(), "Active Employees: %d", dbHelper.getTotalActiveEmployeesCount()));
        List<LeaderboardEntry> topWashers = dbHelper.getTopHandwashers();
        if (!topWashers.isEmpty()) {
            txt_overview_top_washer_today.setText(String.format(Locale.getDefault(), "Top Washer Today: %s (%d washes)", topWashers.get(0).employeeName, topWashers.get(0).handwashCount));
        } else {
            txt_overview_top_washer_today.setText("Top Washer Today: N/A");
        }
        Log.d(TAG, "Admin overview data loaded.");
    }

    private void showDatePickerDialog(final EditText editTextToSetDate, String title) {
        final Calendar c = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                AdminDashboardActivity.this,
                (view, year, monthOfYear, dayOfMonth) -> {
                    String formattedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, monthOfYear + 1, dayOfMonth);
                    editTextToSetDate.setText(formattedDate);
                },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.setTitle(title);
        datePickerDialog.show();
    }

    private void showDownloadDataDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_download_data, null);
        builder.setView(dialogView);
        builder.setTitle("Download Data Options");

        final TextInputEditText dialogEditDownloadStartDate = dialogView.findViewById(R.id.dialog_edit_download_start_date);
        final TextInputEditText dialogEditDownloadEndDate = dialogView.findViewById(R.id.dialog_edit_download_end_date);
        final RadioGroup dialogRadioDownloadType = dialogView.findViewById(R.id.dialog_radio_download_type);
        final Button dialogBtnDownloadConfirm = dialogView.findViewById(R.id.dialog_btn_download_data_confirm);

        dialogEditDownloadStartDate.setOnClickListener(v -> showDatePickerDialog(dialogEditDownloadStartDate, "Select Start Date"));
        dialogEditDownloadEndDate.setOnClickListener(v -> showDatePickerDialog(dialogEditDownloadEndDate, "Select End Date"));

        AlertDialog alertDialog = builder.create();
        dialogBtnDownloadConfirm.setOnClickListener(v -> {
            performDataDownloadFromDialog(Objects.requireNonNull(dialogEditDownloadStartDate.getText()).toString(),
                    Objects.requireNonNull(dialogEditDownloadEndDate.getText()).toString(),
                    dialogRadioDownloadType,
                    alertDialog);
        });
        alertDialog.show();
    }

    private void performDataDownloadFromDialog(String startDate, String endDate, RadioGroup radioGroup, AlertDialog dialogToDismiss) {
        int selectedId = radioGroup.getCheckedRadioButtonId();
        String downloadType;

        if (selectedId == R.id.dialog_radio_summary) {
            downloadType = "summary";
        } else if (selectedId == R.id.dialog_radio_detailed) {
            downloadType = "detailed";
        } else {
            Toast.makeText(this, "Please select a download type.", Toast.LENGTH_SHORT).show();
            txt_message.setText("Download Error: Please select a download type.");
            return;
        }

        if (startDate.isEmpty() || endDate.isEmpty()) {
            Toast.makeText(this, "Please select start and end dates.", Toast.LENGTH_SHORT).show();
            txt_message.setText("Download Error: Please select start and end dates.");
            return;
        }

        List<DatabaseHelper.HandwashLog> logs = dbHelper.getHandwashLogs(startDate, endDate, downloadType);
        if (logs.isEmpty()) {
            txt_message.setText("No data for " + startDate + " to " + endDate + " (" + downloadType + ").");
            Toast.makeText(this, "No data found for download.", Toast.LENGTH_SHORT).show();
            return;
        }

        String csvData = formatDataToCsv(logs, downloadType);
        String fileName = "handwash_" + downloadType + "_" + startDate + "_to_" + endDate + "_" + System.currentTimeMillis() + ".csv";

        this.pendingCsvData = csvData;
        this.pendingFileName = fileName;
        this.pendingDialogToDismiss = dialogToDismiss;

        saveCsvFile(csvData, fileName, dialogToDismiss);
    }

    private void saveCsvFile(String csvData, String fileName, AlertDialog dialogToDismiss) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
                return;
            }
        }
        proceedWithSavingCsv(csvData, fileName, dialogToDismiss);
    }

    private void proceedWithSavingCsv(String csvData, String fileName, AlertDialog dialogToDismiss) {
        Uri fileUri = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                fileUri = getContentResolver().insert(collection, values);
                if (fileUri != null) {
                    try (OutputStream os = getContentResolver().openOutputStream(fileUri)) {
                        if (os != null) os.write(csvData.getBytes());
                        else throw new IOException("OutputStream null for MediaStore URI.");
                    }
                } else throw new IOException("MediaStore insert returned null URI.");
            } else {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists() && !downloadsDir.mkdirs()) throw new IOException("Failed to create public Downloads dir.");
                File csvFile = new File(downloadsDir, fileName);
                try (FileWriter writer = new FileWriter(csvFile)) { writer.write(csvData); }
                fileUri = FileProvider.getUriForFile(this, "com.jarindimick.handwashtracking.fileprovider", csvFile);
            }

            if (fileUri != null) {
                openCsvFile(fileUri);
                txt_message.setText("Data exported to Downloads: " + fileName);
                Toast.makeText(this, "CSV Export successful.", Toast.LENGTH_SHORT).show();
                if (dialogToDismiss != null) dialogToDismiss.dismiss();
            } else throw new IOException("Failed to get valid URI for saved CSV.");
        } catch (IOException e) {
            txt_message.setText("Error saving CSV: " + e.getMessage());
            Log.e(TAG, "Error saving CSV", e);
            Toast.makeText(this, "Error saving CSV: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String formatDataToCsv(List<DatabaseHelper.HandwashLog> logs, String downloadType) {
        StringBuilder csvBuilder = new StringBuilder();
        if (downloadType.equals("summary")) {
            csvBuilder.append("Employee Number,First Name,Last Name,Total Handwashes\n");
            for (DatabaseHelper.HandwashLog log : logs) {
                csvBuilder.append(log.employeeNumber).append(",")
                        .append(log.firstName == null ? "" : log.firstName).append(",")
                        .append(log.lastName == null ? "" : log.lastName).append(",")
                        .append(log.washCount).append("\n");
            }
        } else {
            csvBuilder.append("Employee Number,First Name,Last Name,Wash Date,Wash Time,Photo Path\n");
            for (DatabaseHelper.HandwashLog log : logs) {
                csvBuilder.append(log.employeeNumber).append(",")
                        .append(log.firstName == null ? "" : log.firstName).append(",")
                        .append(log.lastName == null ? "" : log.lastName).append(",")
                        .append(log.washDate).append(",")
                        .append(log.washTime).append(",")
                        .append(log.photoPath == null ? "" : log.photoPath).append("\n");
            }
        }
        return csvBuilder.toString();
    }

    private void openCsvFile(Uri fileUri) {
        Log.d(TAG, "Attempting to open CSV with Uri: " + fileUri.toString());
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, "text/csv");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(Intent.createChooser(intent, "Open CSV with"));
            } else {
                txt_message.setText("No app found to open CSV file.");
                Toast.makeText(this, "No app to open CSV.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            txt_message.setText("Error opening CSV file: " + e.getMessage());
            Log.e(TAG, "Error opening CSV file with Uri " + fileUri.toString(), e);
            Toast.makeText(this, "Could not open CSV file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void searchHandwashes() {
        String firstName = edit_search_first_name.getText().toString().trim();
        String lastName = edit_search_last_name.getText().toString().trim();
        String employeeId = edit_search_employee_id.getText().toString().trim();
        String startDate = edit_search_start_date.getText().toString().trim();
        String endDate = edit_search_end_date.getText().toString().trim();
        if (firstName.isEmpty() && lastName.isEmpty() && employeeId.isEmpty() && startDate.isEmpty() && endDate.isEmpty()) {
            txt_message.setText("Please enter search criteria or a date range.");
            recycler_search_results.setVisibility(View.GONE);
            return;
        }
        List<DatabaseHelper.HandwashLog> results = dbHelper.searchHandwashLogs(firstName, lastName, employeeId, startDate, endDate);
        if (results.isEmpty()) {
            txt_message.setText("No handwash logs found for search criteria.");
            recycler_search_results.setVisibility(View.GONE);
        } else {
            txt_message.setText(String.format(Locale.getDefault(),"Found %d matching log(s).", results.size()));
            recycler_search_results.setVisibility(View.VISIBLE);
            handwashLogAdapter = new HandwashLogAdapter(results, this);
            recycler_search_results.setAdapter(handwashLogAdapter);
        }
    }

    private void logout() {
        new AlertDialog.Builder(this).setTitle("Logout").setMessage("Are you sure?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, MainHandwashing.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }).setNegativeButton("Cancel", null).show();
    }

    private void deleteDataWithConfirmation() {
        final EditText inputStartDate = new EditText(this); inputStartDate.setHint("YYYY-MM-DD (Start)");
        inputStartDate.setFocusable(false); inputStartDate.setClickable(true);
        inputStartDate.setOnClickListener(v -> showDatePickerDialog(inputStartDate, "Select Deletion Start Date"));
        final EditText inputEndDate = new EditText(this); inputEndDate.setHint("YYYY-MM-DD (End)");
        inputEndDate.setFocusable(false); inputEndDate.setClickable(true);
        inputEndDate.setOnClickListener(v -> showDatePickerDialog(inputEndDate, "Select Deletion End Date"));
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(20), dpToPx(10), dpToPx(20), dpToPx(10));
        layout.addView(inputStartDate); layout.addView(inputEndDate);
        new AlertDialog.Builder(this).setTitle("Delete Handwash Logs")
                .setMessage("Select date range to delete. THIS CANNOT BE UNDONE.").setView(layout)
                .setPositiveButton("DELETE", (dialog, which) -> {
                    String startDate = inputStartDate.getText().toString().trim(); String endDate = inputEndDate.getText().toString().trim();
                    if (startDate.isEmpty() || endDate.isEmpty()) {
                        Toast.makeText(this, "Start and End dates are required for deletion.", Toast.LENGTH_LONG).show(); return;
                    }
                    new AlertDialog.Builder(this).setTitle("FINAL CONFIRMATION")
                            .setMessage("REALLY delete logs from " + startDate + " to " + endDate + "?")
                            .setPositiveButton("Yes, Delete Them", (d, w) -> {
                                int rowsDeleted = dbHelper.deleteHandwashLogs(startDate, endDate);
                                txt_message.setText(String.format(Locale.getDefault(),"Deleted %d log(s).", rowsDeleted));
                                Toast.makeText(this, "Deleted " + rowsDeleted + " log(s).", Toast.LENGTH_SHORT).show();
                                loadAdminOverviewData();
                                if (recycler_search_results.getVisibility() == View.VISIBLE) searchHandwashes();
                            }).setNegativeButton("No, Cancel", null).setIcon(android.R.drawable.ic_dialog_alert).show();
                }).setNegativeButton("Cancel", null).setIcon(android.R.drawable.ic_dialog_alert).show();
    }

    private int dpToPx(int dp) { return (int) (dp * getResources().getDisplayMetrics().density); }

    private void importEmployeesFromDevice() {
        String csvFormatInfo = "Please select a CSV file with the following columns in order:<br>" +
                "1. <b>EmployeeNumber</b> (e.g., 101)<br>" +
                "2. <b>FirstName</b> (e.g., John)<br>" +
                "3. <b>LastName</b> (e.g., Doe)<br>" +
                "4. <b>Department</b> (e.g., Kitchen) <i>(Optional, defaults to 'Imported')</i><br><br>" +
                "No header row is expected in the CSV file.";
        new AlertDialog.Builder(this)
                .setTitle("CSV Import Information")
                .setMessage(Html.fromHtml(csvFormatInfo, Html.FROM_HTML_MODE_LEGACY))
                .setPositiveButton("Select File", (dialog, which) -> launchCsvFilePicker())
                .setNegativeButton("Cancel", null).show();
    }

    private void launchCsvFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.setType("text/csv"); // Primary suggestion
        String[] mimeTypes = {
                "text/csv",
                "text/comma-separated-values",
                "application/csv",
                "application/vnd.ms-excel" // Sometimes CSVs are associated with Excel MIME type
                // You could also add "text/plain" if CSVs are often treated as plain text on the device
        };
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        try {
            startActivityForResult(Intent.createChooser(intent, "Select a CSV file"), REQUEST_CODE_SELECT_CSV);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
            txt_message.setText("No File Manager found to select CSV.");
        }
    }

    private void processSelectedCsv(Uri csvFileUri) {
        List<EmployeeFromCsv> employeesToImport;
        try {
            employeesToImport = readEmployeesFromCsvUri(csvFileUri);
        } catch (IOException e) {
            txt_message.setText("Error reading selected CSV: " + e.getMessage()); Log.e(TAG, "Error reading CSV URI", e); return;
        }
        if (employeesToImport.isEmpty()) {
            txt_message.setText("Selected CSV is empty or has no valid data."); return;
        }
        int newCount = 0, skippedCount = 0, errorCount = 0;
        for (EmployeeFromCsv empData : employeesToImport) {
            if (dbHelper.isEmployeeNumberTaken(empData.employeeNumber)) {
                skippedCount++;
            } else {
                if (dbHelper.insertEmployee(empData.employeeNumber, empData.firstName, empData.lastName, empData.department) > 0) newCount++; else errorCount++;
            }
        }
        txt_message.setText(String.format(Locale.getDefault(), "CSV Import: New: %d, Skipped: %d, Errors: %d", newCount, skippedCount, errorCount));
        Toast.makeText(this, "Employee import from file finished.", Toast.LENGTH_SHORT).show();
        loadAdminOverviewData();
    }

    private List<EmployeeFromCsv> readEmployeesFromCsvUri(Uri csvFileUri) throws IOException {
        List<EmployeeFromCsv> employees = new ArrayList<>();
        try (InputStream inputStream = getContentResolver().openInputStream(csvFileUri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] tokens = line.split(",");
                if (tokens.length >= 3) {
                    String empNo = tokens[0].trim(); String fName = tokens[1].trim(); String lName = tokens[2].trim();
                    String dept = (tokens.length >= 4) ? tokens[3].trim() : "Imported";
                    if (!empNo.isEmpty() && !fName.isEmpty() && !lName.isEmpty()) employees.add(new EmployeeFromCsv(empNo, fName, lName, dept));
                    else Log.w(TAG, "Skipping CSV line (URI) with missing data: " + line);
                } else Log.w(TAG, "Skipping malformed CSV line (URI): " + line);
            }
        } catch (NullPointerException e) { throw new IOException("Failed to open input stream from URI.", e); }
        return employees;
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);
        final TextInputEditText oldPass = dialogView.findViewById(R.id.dialog_edit_old_password);
        final TextInputEditText newPass = dialogView.findViewById(R.id.dialog_edit_new_password);
        final TextInputEditText confirmNewPass = dialogView.findViewById(R.id.dialog_edit_confirm_new_password);
        builder.setTitle("Change Admin Password");
        builder.setPositiveButton("Save", null);
        builder.setNegativeButton("Cancel", (d, w) -> d.dismiss());
        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                String o = Objects.requireNonNull(oldPass.getText()).toString(), n = Objects.requireNonNull(newPass.getText()).toString(), c = Objects.requireNonNull(confirmNewPass.getText()).toString();
                if (o.isEmpty() || n.isEmpty() || c.isEmpty()) { Toast.makeText(this, "All password fields required.", Toast.LENGTH_SHORT).show(); return; }
                if (n.length() < 6) { Toast.makeText(this, "New password too short (min 6).", Toast.LENGTH_SHORT).show(); return; }
                if (!n.equals(c)) { Toast.makeText(this, "New passwords don't match.", Toast.LENGTH_SHORT).show(); return; }
                if (dbHelper.validateAdminLogin("admin", o)) {
                    if (dbHelper.updateAdminPassword("admin", n)) {
                        Toast.makeText(this, "Password changed.", Toast.LENGTH_SHORT).show();
                        txt_message.setText("Admin password updated.");
                        alertDialog.dismiss();
                    } else txt_message.setText("DB error changing password.");
                } else Toast.makeText(this, "Old password incorrect.", Toast.LENGTH_SHORT).show();
            });
        });
        alertDialog.show();
    }

    private void showAddEmployeeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_employee, null);
        builder.setView(dialogView);
        final TextInputEditText empNo = dialogView.findViewById(R.id.dialog_edit_add_employee_number);
        final TextInputEditText fName = dialogView.findViewById(R.id.dialog_edit_add_first_name);
        final TextInputEditText lName = dialogView.findViewById(R.id.dialog_edit_add_last_name);
        final TextInputEditText dept = dialogView.findViewById(R.id.dialog_edit_add_department);
        builder.setTitle("Add New Employee (Quick Add)");
        builder.setPositiveButton("Add", null);
        builder.setNegativeButton("Cancel", (d,w) -> d.dismiss());
        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                String en = Objects.requireNonNull(empNo.getText()).toString().trim(), fn=Objects.requireNonNull(fName.getText()).toString().trim(), ln=Objects.requireNonNull(lName.getText()).toString().trim();
                String dpt = Objects.requireNonNull(dept.getText()).toString().trim().isEmpty() ? "Unassigned" : Objects.requireNonNull(dept.getText()).toString().trim();
                if (en.isEmpty() || fn.isEmpty() || ln.isEmpty()) { Toast.makeText(this, "Emp No, First & Last Name required.", Toast.LENGTH_SHORT).show(); return; }
                if (dbHelper.isEmployeeNumberTaken(en)) { Toast.makeText(this, "Employee number " + en + " exists.", Toast.LENGTH_SHORT).show(); return; }
                if (dbHelper.insertEmployee(en, fn, ln, dpt) != -1) {
                    Toast.makeText(this, "Employee " + fn + " added.", Toast.LENGTH_SHORT).show();
                    txt_message.setText("Quick Added: " + fn + " " + ln);
                    loadAdminOverviewData();
                    alertDialog.dismiss();
                } else Toast.makeText(this, "Error adding employee.", Toast.LENGTH_SHORT).show();
            });
        });
        alertDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "WRITE_EXTERNAL_STORAGE permission granted for CSV download.");
                if (pendingCsvData != null && pendingFileName != null) {
                    proceedWithSavingCsv(pendingCsvData, pendingFileName, pendingDialogToDismiss);
                }
            } else {
                Log.w(TAG, "WRITE_EXTERNAL_STORAGE permission denied for CSV download.");
                Toast.makeText(this, "Storage permission denied. Cannot save CSV to public Downloads.", Toast.LENGTH_LONG).show();
                txt_message.setText("Storage permission needed to save to Downloads folder.");
            }
            pendingCsvData = null; pendingFileName = null; pendingDialogToDismiss = null;
        }
    }

    private static class EmployeeFromCsv {
        String employeeNumber, firstName, lastName, department;
        public EmployeeFromCsv(String en, String fn, String ln, String d) {
            employeeNumber=en; firstName=fn; lastName=ln; department=d;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}