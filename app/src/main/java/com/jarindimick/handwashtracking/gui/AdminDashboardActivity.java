package com.jarindimick.handwashtracking.gui;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
// Make sure TextInputEditText is used if your layouts use it, or EditText if they use plain EditText
import com.google.android.material.textfield.TextInputEditText; // Or android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.widget.NestedScrollView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
// TextInputEditText already imported above
import com.jarindimick.handwashtracking.R;
import com.jarindimick.handwashtracking.databasehelper.DatabaseHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class AdminDashboardActivity extends AppCompatActivity {
    private static final String TAG = "AdminDashboardActivity";
    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 101;
    private static final int REQUEST_CODE_SELECT_LOGO_IMAGE = 103;

    public static final String PREFS_NAME = "HandwashAppPrefs";
    public static final String KEY_CUSTOM_LOGO_PATH = "custom_logo_path";
    public static final String CUSTOM_LOGO_FILENAME = "custom_app_logo.png";

    public static final String PREFS_ADMIN_TOUR_FILE = "AdminDashboardTourPrefs";
    public static final String KEY_ADMIN_INTERACTIVE_TOUR_SHOWN = "adminInteractiveTourShown";
    private boolean adminTourAttemptedThisSession = false;

    private ArrayList<TapTarget> tourTargetsList = new ArrayList<>();
    private ArrayList<View> tourTargetViewsList = new ArrayList<>();

    private TextView txt_overview_total_washes_today;
    private TextView txt_overview_active_employees;
    private TextView txt_overview_top_washer_today;
    private TextInputEditText edit_search_first_name;
    private TextInputEditText edit_search_last_name;
    private TextInputEditText edit_search_employee_id;
    private TextInputEditText edit_search_start_date;
    private TextInputEditText edit_search_end_date;
    private MaterialButton btn_search_handwashes;
    private RecyclerView recycler_search_results;
    private HandwashLogAdapter handwashLogAdapter;
    private TextView txt_message;
    private MaterialToolbar toolbarAdminDashboard;

    private MaterialButton btn_go_to_manage_employees;
    private MaterialButton btn_upload_logo;
    private MaterialButton btn_show_download_data_dialog;

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
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        dbHelper = new DatabaseHelper(this);
        setupgui();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAdminOverviewData();
        // adminTourAttemptedThisSession = false; // Optional: Reset to allow tour to re-attempt if focus is gained again
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.admin_dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_change_password_toolbar) {
            showChangePasswordDialog();
            return true;
        } else if (itemId == R.id.action_logout_toolbar) {
            logout();
            return true;
        } else if (itemId == R.id.action_delete_data_range_toolbar) {
            deleteDataWithConfirmation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && !adminTourAttemptedThisSession) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (hasWindowFocus()) {
                    showAdminInteractiveTourIfNeeded();
                }
            }, 700);
        }
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
        txt_message = findViewById(R.id.txt_message);
        btn_go_to_manage_employees = findViewById(R.id.btn_go_to_manage_employees);
        btn_upload_logo = findViewById(R.id.btn_upload_logo);
        btn_show_download_data_dialog = findViewById(R.id.btn_show_download_data_dialog);
    }

    private void setupListeners() {
        edit_search_start_date.setOnClickListener(v -> showDatePickerDialog(edit_search_start_date, "Set Search Start Date"));
        edit_search_end_date.setOnClickListener(v -> showDatePickerDialog(edit_search_end_date, "Set Search End Date"));
        btn_search_handwashes.setOnClickListener(v -> searchHandwashes());

        btn_upload_logo.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String currentLogoPath = prefs.getString(KEY_CUSTOM_LOGO_PATH, null);
            File logoFile = null;
            if (currentLogoPath != null) {
                logoFile = new File(currentLogoPath);
            }

            if (logoFile != null && logoFile.exists()) {
                new AlertDialog.Builder(AdminDashboardActivity.this)
                        .setTitle("App Logo Options")
                        .setItems(new CharSequence[]{"Change Logo", "Remove Current Logo", "Cancel"}, (dialog, which) -> {
                            switch (which) {
                                case 0: // Change Logo
                                    openImagePicker();
                                    break;
                                case 1: // Remove Current Logo
                                    removeCustomLogoWithConfirmation();
                                    break;
                                case 2: // Cancel
                                    dialog.dismiss();
                                    break;
                            }
                        })
                        .show();
            } else {
                // No logo exists, or path is invalid, proceed to pick new one
                openImagePicker();
            }
        });

        btn_go_to_manage_employees.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, ManageEmployeesActivity.class);
            startActivity(intent);
        });
        btn_show_download_data_dialog.setOnClickListener(v -> showDownloadDataDialog());
    }

    private void scrollToView(final View view) {
        NestedScrollView nestedScrollView = findViewById(R.id.main_scrollview_admin_dashboard);
        if (view == null || nestedScrollView == null) {
            Log.w(TAG, "scrollToView: View or NestedScrollView is null.");
            return;
        }
        view.post(() -> {
            Rect rect = new Rect(0, 0, view.getWidth(), view.getHeight());
            boolean fullyVisible = view.requestRectangleOnScreen(rect, false);
            Log.d(TAG, "View " + view.getResources().getResourceEntryName(view.getId()) + " requested on screen. Fully visible: " + fullyVisible);
        });
    }

    private void showAdminInteractiveTourIfNeeded() {
        final SharedPreferences tourPrefs = getSharedPreferences(PREFS_ADMIN_TOUR_FILE, MODE_PRIVATE);
        boolean tourPermanentlyShown = tourPrefs.getBoolean(KEY_ADMIN_INTERACTIVE_TOUR_SHOWN, false);

        if (tourPermanentlyShown) {
            Log.d(TAG, "Admin interactive tour already permanently shown.");
            return;
        }
        if (adminTourAttemptedThisSession && !tourPermanentlyShown) {
            Log.d(TAG, "Admin interactive tour already attempted this session or prerequisites were not met earlier.");
            return;
        }
        adminTourAttemptedThisSession = true;

        View overviewCard = findViewById(R.id.card_overview);
        View manageEmployeesButton = findViewById(R.id.btn_go_to_manage_employees);
        View appLogoButton = findViewById(R.id.btn_upload_logo);
        View searchButton = findViewById(R.id.btn_search_handwashes);
        View downloadButton = findViewById(R.id.btn_show_download_data_dialog);

        if (toolbarAdminDashboard == null || overviewCard == null || manageEmployeesButton == null ||
                appLogoButton == null || searchButton == null || downloadButton == null) {
            Log.w(TAG, "Admin tour prerequisites not met (some views are null). Tour skipped for this focus attempt.");
            adminTourAttemptedThisSession = false;
            return;
        }
        if (toolbarAdminDashboard.getMenu() == null || toolbarAdminDashboard.getMenu().size() == 0) {
            Log.w(TAG, "Admin tour: Toolbar menu not ready yet. Tour skipped for this focus attempt.");
            adminTourAttemptedThisSession = false;
            return;
        }

        tourTargetsList.clear();
        tourTargetViewsList.clear();
        ArrayList<TapTarget> localTargets = new ArrayList<>();

        try {
            localTargets.add(TapTarget.forToolbarOverflow(toolbarAdminDashboard, "Admin Options", "Tap here for settings like changing your password, deleting data ranges, or logging out.")
                    .outerCircleColor(R.color.purple_500).outerCircleAlpha(0.70f)
                    .targetCircleColor(android.R.color.white).titleTextColor(android.R.color.white)
                    .descriptionTextColor(android.R.color.white).textTypeface(Typeface.SANS_SERIF)
                    .dimColor(R.color.tour_dim_background).drawShadow(true)
                    .cancelable(false).targetRadius(40).id(1)); // Smaller radius for toolbar


            localTargets.add(TapTarget.forView(manageEmployeesButton, "Manage Your Staff", "Tap here to add new employees, edit their details, or remove them. You can also import a list of employees.")
                    .outerCircleColor(R.color.purple_500).outerCircleAlpha(0.70f)
                    .targetCircleColor(android.R.color.white).titleTextColor(android.R.color.white)
                    .descriptionTextColor(android.R.color.white).textTypeface(Typeface.SANS_SERIF)
                    .dimColor(R.color.tour_dim_background).drawShadow(true)
                    .cancelable(false).targetRadius(40).id(2));
            tourTargetViewsList.add(manageEmployeesButton);


            localTargets.add(TapTarget.forView(searchButton, "Find Past Handwashes", "Use the fields above this area to search for specific records")
                    .outerCircleColor(R.color.purple_500).outerCircleAlpha(0.75f)
                    .targetCircleColor(android.R.color.white).titleTextColor(android.R.color.white)
                    .descriptionTextColor(android.R.color.white).textTypeface(Typeface.SANS_SERIF)
                    .dimColor(R.color.tour_dim_background).drawShadow(true)
                    .cancelable(false).targetRadius(20).id(3));
            tourTargetViewsList.add(searchButton);

            localTargets.add(TapTarget.forView(downloadButton, "Download Reports", "Generate and download handwash reports")
                    .outerCircleColor(R.color.purple_500).outerCircleAlpha(0.75f)
                    .targetCircleColor(android.R.color.white).titleTextColor(android.R.color.white)
                    .descriptionTextColor(android.R.color.white).textTypeface(Typeface.SANS_SERIF)
                    .dimColor(R.color.tour_dim_background).drawShadow(true)
                    .cancelable(false).targetRadius(20).id(4));
            tourTargetViewsList.add(downloadButton);

        } catch (Exception e) {
            Log.e(TAG, "Error creating TapTarget for Admin Dashboard tour", e);
            SharedPreferences.Editor editor = tourPrefs.edit();
            editor.putBoolean(KEY_ADMIN_INTERACTIVE_TOUR_SHOWN, true); editor.apply(); return;
        }

        this.tourTargetsList.addAll(localTargets); // Populate member list

        TapTargetSequence.Listener sequenceListener = new TapTargetSequence.Listener() {
            @Override public void onSequenceFinish() {
                Log.d(TAG, "Admin interactive tour sequence finished.");
                SharedPreferences.Editor editor = tourPrefs.edit();
                editor.putBoolean(KEY_ADMIN_INTERACTIVE_TOUR_SHOWN, true); editor.apply();
                tourTargetViewsList.clear(); tourTargetsList.clear();
            }

            @Override public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {
                int lastTargetId = lastTarget.id();
                int nextTargetToShowIndexInFullList = -1;
                for (int i = 0; i < tourTargetsList.size(); i++) {
                    if (tourTargetsList.get(i).id() == lastTargetId) {
                        nextTargetToShowIndexInFullList = i + 1;
                        break;
                    }
                }
                if (nextTargetToShowIndexInFullList > 0 && nextTargetToShowIndexInFullList < tourTargetsList.size()) {
                    int viewListIndex = nextTargetToShowIndexInFullList - 1;
                    if (viewListIndex >= 0 && viewListIndex < tourTargetViewsList.size()) {
                        View nextView = tourTargetViewsList.get(viewListIndex);
                        if (nextView != null) {
                            Log.d(TAG, "Tour step: Scrolling to " + nextView.getResources().getResourceEntryName(nextView.getId()));
                            new Handler(Looper.getMainLooper()).postDelayed(() -> scrollToView(nextView), 150);
                        }
                    }
                }
            }
            @Override public void onSequenceCanceled(TapTarget lastTarget) {
                Log.w(TAG, "Admin interactive tour sequence cancelled.");
                SharedPreferences.Editor editor = tourPrefs.edit();
                editor.putBoolean(KEY_ADMIN_INTERACTIVE_TOUR_SHOWN, true); editor.apply();
                tourTargetViewsList.clear(); tourTargetsList.clear();
            }
        };

        if (!this.tourTargetsList.isEmpty()) {
            new TapTargetSequence(this).targets(this.tourTargetsList).listener(sequenceListener)
                    .continueOnCancel(false).considerOuterCircleCanceled(true).start();
            Log.d(TAG, "Admin interactive tour sequence started.");
        } else {
            Log.w(TAG, "No targets were created for the admin tour.");
            SharedPreferences.Editor editor = tourPrefs.edit();
            editor.putBoolean(KEY_ADMIN_INTERACTIVE_TOUR_SHOWN, true); editor.apply();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        try {
            startActivityForResult(intent, REQUEST_CODE_SELECT_LOGO_IMAGE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager or Gallery app.", Toast.LENGTH_SHORT).show();
            txt_message.setText("No app found to select an image.");
        }
    }

    private void removeCustomLogoWithConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Remove Logo")
                .setMessage("Are you sure you want to remove the custom app logo? This will revert to no logo on the main screen.")
                .setPositiveButton("Remove", (dialog, which) -> performLogoRemoval())
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void performLogoRemoval() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        File internalDir = getFilesDir();
        File logoFile = new File(internalDir, CUSTOM_LOGO_FILENAME);
        boolean fileActionSuccess = false;
        if (logoFile.exists()) {
            fileActionSuccess = logoFile.delete();
            if (!fileActionSuccess) Log.e(TAG, "Failed to delete custom logo file: " + logoFile.getAbsolutePath());
        } else {
            Log.d(TAG, "Custom logo file not found (already removed or never existed): " + logoFile.getAbsolutePath());
            fileActionSuccess = true;
        }
        if (fileActionSuccess) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(KEY_CUSTOM_LOGO_PATH);
            editor.apply();
            txt_message.setText("Custom app logo removed. Change will be visible on the main screen on its next load/restart.");
            Toast.makeText(this, "Custom logo removed.", Toast.LENGTH_SHORT).show();
        } else {
            txt_message.setText("Error: Could not remove the custom logo file.");
            Toast.makeText(this, "Failed to remove logo file.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveLogoToAppStorage(Uri sourceUri) {
        if (sourceUri == null) {
            Toast.makeText(this, "Failed to get image.", Toast.LENGTH_SHORT).show(); return;
        }
        File internalDir = getFilesDir();
        File logoFile = new File(internalDir, CUSTOM_LOGO_FILENAME);
        try (InputStream inputStream = getContentResolver().openInputStream(sourceUri);
             OutputStream outputStream = new FileOutputStream(logoFile)) {
            if (inputStream == null) throw new IOException("Unable to open input stream from URI");
            byte[] buffer = new byte[1024]; int length;
            while ((length = inputStream.read(buffer)) > 0) outputStream.write(buffer, 0, length);
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_CUSTOM_LOGO_PATH, logoFile.getAbsolutePath());
            editor.apply();
            txt_message.setText("Logo updated. It will show on the main screen.");
            Toast.makeText(this, "App logo updated successfully!", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "Error saving logo: " + e.getMessage(), e);
            txt_message.setText("Error saving logo to app storage.");
            Toast.makeText(this, "Error saving logo. Please try again.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_LOGO_IMAGE) {
            if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
                Uri selectedImageUri = data.getData();
                txt_message.setText("Processing selected logo...");
                saveLogoToAppStorage(selectedImageUri);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                txt_message.setText("Logo selection cancelled.");
            } else {
                txt_message.setText("Failed to select logo image.");
                Toast.makeText(this, "Failed to get image.", Toast.LENGTH_SHORT).show();
            }
        }
    }

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

    private void showDatePickerDialog(final TextInputEditText editTextToSetDate, String title) {
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
        if (selectedId == R.id.dialog_radio_summary) downloadType = "summary";
        else if (selectedId == R.id.dialog_radio_detailed) downloadType = "detailed";
        else { txt_message.setText("Download Error: Please select a download type."); return; }
        if (startDate.isEmpty() || endDate.isEmpty()) {
            txt_message.setText("Download Error: Please select start and end dates."); return;
        }
        List<DatabaseHelper.HandwashLog> logs = dbHelper.getHandwashLogs(startDate, endDate, downloadType);
        if (logs.isEmpty()) {
            txt_message.setText("No data for " + startDate + " to " + endDate + " (" + downloadType + ")."); return;
        }
        String csvData = formatDataToCsv(logs, downloadType);
        String fileName = "handwash_" + downloadType + "_" + startDate + "_to_" + endDate + "_" + System.currentTimeMillis() + ".csv";
        this.pendingCsvData = csvData; this.pendingFileName = fileName; this.pendingDialogToDismiss = dialogToDismiss;
        saveCsvFile(csvData, fileName, dialogToDismiss);
    }

    private void saveCsvFile(String csvData, String fileName, AlertDialog dialogToDismiss) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
            return;
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
                        if (os != null) os.write(csvData.getBytes()); else throw new IOException("OutputStream null for MediaStore URI.");
                    }
                } else throw new IOException("MediaStore insert returned null URI.");
            } else {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists() && !downloadsDir.mkdirs()) throw new IOException("Failed to create public Downloads dir.");
                File csvFile = new File(downloadsDir, fileName);
                try (FileWriter writer = new FileWriter(csvFile)) { writer.write(csvData); }
                fileUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", csvFile);
            }
            if (fileUri != null) {
                openCsvFile(fileUri);
                txt_message.setText("Data exported to Downloads: " + fileName);
                if (dialogToDismiss != null) dialogToDismiss.dismiss();
            } else throw new IOException("Failed to get valid URI for saved CSV.");
        } catch (IOException | IllegalArgumentException e) {
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
                csvBuilder.append(log.employeeNumber).append(",").append(log.firstName == null ? "" : log.firstName).append(",").append(log.lastName == null ? "" : log.lastName).append(",").append(log.washCount).append("\n");
            }
        } else {
            csvBuilder.append("Employee Number,First Name,Last Name,Wash Date,Wash Time,Photo Path\n");
            for (DatabaseHelper.HandwashLog log : logs) {
                csvBuilder.append(log.employeeNumber).append(",").append(log.firstName == null ? "" : log.firstName).append(",").append(log.lastName == null ? "" : log.lastName).append(",").append(log.washDate).append(",").append(log.washTime).append(",").append(log.photoPath == null ? "" : log.photoPath).append("\n");
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
        String firstName = Objects.requireNonNull(edit_search_first_name.getText()).toString().trim();
        String lastName = Objects.requireNonNull(edit_search_last_name.getText()).toString().trim();
        String employeeId = Objects.requireNonNull(edit_search_employee_id.getText()).toString().trim();
        String startDate = Objects.requireNonNull(edit_search_start_date.getText()).toString().trim();
        String endDate = Objects.requireNonNull(edit_search_end_date.getText()).toString().trim();
        if (firstName.isEmpty() && lastName.isEmpty() && employeeId.isEmpty() && startDate.isEmpty() && endDate.isEmpty()) {
            txt_message.setText("Please enter search criteria or a date range.");
            recycler_search_results.setVisibility(View.GONE); return;
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
                    finishAffinity();
                }).setNegativeButton("Cancel", null).show();
    }

    private void deleteDataWithConfirmation() {
        final TextInputEditText inputStartDate = new TextInputEditText(this);
        inputStartDate.setHint("YYYY-MM-DD (Start)");
        inputStartDate.setFocusable(false); inputStartDate.setClickable(true);
        inputStartDate.setOnClickListener(v -> showDatePickerDialog(inputStartDate, "Select Deletion Start Date"));
        final TextInputEditText inputEndDate = new TextInputEditText(this);
        inputEndDate.setHint("YYYY-MM-DD (End)");
        inputEndDate.setFocusable(false); inputEndDate.setClickable(true);
        inputEndDate.setOnClickListener(v -> showDatePickerDialog(inputEndDate, "Select Deletion End Date"));
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL);
        int padding = dpToPx(20); layout.setPadding(padding, dpToPx(10), padding, dpToPx(10));
        layout.addView(inputStartDate); layout.addView(inputEndDate);
        new AlertDialog.Builder(this).setTitle("Delete Handwash Logs")
                .setMessage("Select date range to delete. THIS CANNOT BE UNDONE.").setView(layout)
                .setPositiveButton("DELETE", (dialog, which) -> {
                    String startDateStr = Objects.requireNonNull(inputStartDate.getText()).toString().trim();
                    String endDateStr = Objects.requireNonNull(inputEndDate.getText()).toString().trim();
                    if (startDateStr.isEmpty() || endDateStr.isEmpty()) {
                        txt_message.setText("Start and End dates are required for deletion."); return;
                    }
                    new AlertDialog.Builder(this).setTitle("FINAL CONFIRMATION")
                            .setMessage("REALLY delete logs from " + startDateStr + " to " + endDateStr + "?")
                            .setPositiveButton("Yes, Delete Them", (d, w) -> {
                                int rowsDeleted = dbHelper.deleteHandwashLogs(startDateStr, endDateStr);
                                txt_message.setText(String.format(Locale.getDefault(),"Deleted %d log(s) from %s to %s.", rowsDeleted, startDateStr, endDateStr));
                                loadAdminOverviewData();
                                if (recycler_search_results.getVisibility() == View.VISIBLE) searchHandwashes();
                            }).setNegativeButton("No, Cancel", null).setIcon(android.R.drawable.ic_dialog_alert).show();
                }).setNegativeButton("Cancel", null).setIcon(android.R.drawable.ic_dialog_alert).show();
    }

    private int dpToPx(int dp) { return (int) (dp * getResources().getDisplayMetrics().density); }

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
                String o = Objects.requireNonNull(oldPass.getText()).toString(); String n = Objects.requireNonNull(newPass.getText()).toString(); String c = Objects.requireNonNull(confirmNewPass.getText()).toString();
                oldPass.setError(null); newPass.setError(null); confirmNewPass.setError(null);
                boolean passValid = true;
                if (o.isEmpty()) { oldPass.setError("Old password required."); passValid = false; }
                if (n.isEmpty()) { newPass.setError("New password required."); passValid = false; }
                if (c.isEmpty()) { confirmNewPass.setError("Confirm new password."); passValid = false; }
                if (!passValid) return;
                if (n.length() < 6) { newPass.setError("New password too short (min 6)."); return; }
                if (!n.equals(c)) { confirmNewPass.setError("New passwords don't match."); return; }
                if (dbHelper.validateAdminLogin("admin", o)) {
                    if (dbHelper.updateAdminPassword("admin", n)) {
                        txt_message.setText("Admin password updated.");
                        Toast.makeText(this, "Password changed.", Toast.LENGTH_SHORT).show();
                        alertDialog.dismiss();
                    } else {
                        txt_message.setText("DB error changing password.");
                        Toast.makeText(this, "Database error.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    oldPass.setError("Old password incorrect.");
                }
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
                txt_message.setText("Storage permission needed to save to Downloads folder.");
                Toast.makeText(this, "Storage permission denied. Cannot save CSV to public Downloads.", Toast.LENGTH_LONG).show();
            }
            pendingCsvData = null; pendingFileName = null; pendingDialogToDismiss = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}