package com.jarindimick.handwashtracking.databasehelper;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.util.Log;

import com.jarindimick.handwashtracking.gui.Employee; // Corrected import
import com.jarindimick.handwashtracking.gui.LeaderboardEntry;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.mindrot.jbcrypt.BCrypt;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "Handwash.db";
    private static final int DATABASE_VERSION = 3; // Or increment if schema changes

    public static final String TABLE_EMPLOYEES = "employees";
    public static final String TABLE_HANDWASH_LOG = "handwash_log";
    public static final String TABLE_ADMIN_USERS = "admin_users";

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_EMPLOYEE_NUMBER = "employee_number";
    public static final String COLUMN_FIRST_NAME = "first_name";
    public static final String COLUMN_LAST_NAME = "last_name";
    public static final String COLUMN_DEPARTMENT = "department";
    public static final String COLUMN_IS_ACTIVE = "is_active";

    public static final String COLUMN_WASH_DATE = "wash_date";
    public static final String COLUMN_WASH_TIME = "wash_time";
    public static final String COLUMN_PHOTO_PATH = "photo_path";

    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_PASSWORD_HASH = "password_hash";
    public static final String COLUMN_ROLE = "role";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Creating database tables...");
        String CREATE_EMPLOYEES_TABLE = "CREATE TABLE " + TABLE_EMPLOYEES + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_EMPLOYEE_NUMBER + " TEXT UNIQUE NOT NULL,"
                + COLUMN_FIRST_NAME + " TEXT,"
                + COLUMN_LAST_NAME + " TEXT,"
                + COLUMN_DEPARTMENT + " TEXT,"
                + COLUMN_IS_ACTIVE + " INTEGER DEFAULT 1" + ")";
        db.execSQL(CREATE_EMPLOYEES_TABLE);

        String CREATE_HANDWASH_LOG_TABLE = "CREATE TABLE " + TABLE_HANDWASH_LOG + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_EMPLOYEE_NUMBER + " TEXT,"
                + COLUMN_WASH_DATE + " TEXT,"
                + COLUMN_WASH_TIME + " TEXT,"
                + COLUMN_PHOTO_PATH + " TEXT" + ")";
        db.execSQL(CREATE_HANDWASH_LOG_TABLE);

        String CREATE_ADMIN_USERS_TABLE = "CREATE TABLE " + TABLE_ADMIN_USERS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_USERNAME + " TEXT UNIQUE NOT NULL,"
                + COLUMN_PASSWORD_HASH + " TEXT NOT NULL,"
                + COLUMN_ROLE + " TEXT" + ")";
        db.execSQL(CREATE_ADMIN_USERS_TABLE);

        String defaultPassword = "admin";
        String hashedPassword = BCrypt.hashpw(defaultPassword, BCrypt.gensalt());
        ContentValues adminValues = new ContentValues();
        adminValues.put(COLUMN_USERNAME, "admin");
        adminValues.put(COLUMN_PASSWORD_HASH, hashedPassword);
        adminValues.put(COLUMN_ROLE, "administrator");
        db.insert(TABLE_ADMIN_USERS, null, adminValues);
        Log.d(TAG, "Database tables created and initial admin user (admin/admin) inserted.");

        // --- START: Add Guest User ---
        ContentValues guestValues = new ContentValues();
        guestValues.put(COLUMN_EMPLOYEE_NUMBER, "0");
        guestValues.put(COLUMN_FIRST_NAME, "Guest");
        guestValues.put(COLUMN_LAST_NAME, ""); // Last name can be empty or a placeholder
        guestValues.put(COLUMN_DEPARTMENT, "N/A");
        guestValues.put(COLUMN_IS_ACTIVE, 1); // Guest user is always active for logging
        db.insert(TABLE_EMPLOYEES, null, guestValues);
        Log.d(TAG, "Default Guest user (0) inserted.");
        // --- END: Add Guest User ---
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EMPLOYEES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HANDWASH_LOG);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ADMIN_USERS);
        onCreate(db);
    }

    // --- Method to update an employee ---
    public boolean updateEmployee(Employee employee) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_FIRST_NAME, employee.getFirstName());
        values.put(COLUMN_LAST_NAME, employee.getLastName());
        values.put(COLUMN_DEPARTMENT, employee.getDepartment());
        values.put(COLUMN_IS_ACTIVE, employee.isActive() ? 1 : 0);
        // Employee number is the key and should not be changed here.
        // We update based on the employee's internal ID.

        int rowsAffected = 0;
        try {
            rowsAffected = db.update(TABLE_EMPLOYEES, values, COLUMN_ID + " = ?",
                    new String[]{String.valueOf(employee.getId())});
        } catch (Exception e) {
            Log.e(TAG, "Error updating employee: " + e.getMessage());
        } finally {
            db.close();
        }
        return rowsAffected > 0;
    }
    // --- End update employee method ---

    public List<Employee> getAllEmployees() {
        List<Employee> employeeList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_EMPLOYEES,
                    new String[]{COLUMN_ID, COLUMN_EMPLOYEE_NUMBER, COLUMN_FIRST_NAME, COLUMN_LAST_NAME, COLUMN_DEPARTMENT, COLUMN_IS_ACTIVE},
                    null, null, null, null, COLUMN_LAST_NAME + " ASC, " + COLUMN_FIRST_NAME + " ASC");

            if (cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                    String number = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMPLOYEE_NUMBER));
                    String firstName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRST_NAME));
                    String lastName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_NAME));
                    String department = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DEPARTMENT));
                    boolean isActive = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_ACTIVE)) == 1;
                    employeeList.add(new Employee(id, number, firstName, lastName, department, isActive));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to get employees from database", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return employeeList;
    }


    public int getTotalHandwashesToday() {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());
        String today = currentDate.format(formatter);
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_HANDWASH_LOG +
                    " WHERE " + COLUMN_WASH_DATE + " = ?", new String[]{today});
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting total handwashes today: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return count;
    }

    public int getTotalActiveEmployeesCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_EMPLOYEES +
                    " WHERE " + COLUMN_IS_ACTIVE + " = 1", null);
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting total active employees: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return count;
    }

    public boolean isEmployeeNumberTaken(String employeeNumber) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        boolean isTaken = false;
        try {
            cursor = db.query(TABLE_EMPLOYEES, new String[]{COLUMN_ID},
                    COLUMN_EMPLOYEE_NUMBER + " = ?",
                    new String[]{employeeNumber}, null, null, null);
            isTaken = (cursor.getCount() > 0);
        } catch (Exception e) {
            Log.e(TAG, "Error checking if employee number is taken: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return isTaken;
    }


    public List<HandwashLog> searchHandwashLogs(String firstName, String lastName, String employeeNumber, String startDate, String endDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<HandwashLog> logs = new ArrayList<>();
        StringBuilder queryBuilder = new StringBuilder("SELECT hl.*, e." + COLUMN_FIRST_NAME + ", e." + COLUMN_LAST_NAME +
                " FROM " + TABLE_HANDWASH_LOG + " hl " +
                " LEFT JOIN " + TABLE_EMPLOYEES + " e ON hl." + COLUMN_EMPLOYEE_NUMBER + " = e." + COLUMN_EMPLOYEE_NUMBER);
        List<String> whereArgs = new ArrayList<>();
        boolean hasWhere = false;

        if (firstName != null && !firstName.isEmpty()) {
            queryBuilder.append(hasWhere ? " AND " : " WHERE ").append("e." + COLUMN_FIRST_NAME + " LIKE ?");
            whereArgs.add("%" + firstName + "%");
            hasWhere = true;
        }
        if (lastName != null && !lastName.isEmpty()) {
            queryBuilder.append(hasWhere ? " AND " : " WHERE ").append("e." + COLUMN_LAST_NAME + " LIKE ?");
            whereArgs.add("%" + lastName + "%");
            hasWhere = true;
        }
        if (employeeNumber != null && !employeeNumber.isEmpty()) {
            queryBuilder.append(hasWhere ? " AND " : " WHERE ").append("hl." + COLUMN_EMPLOYEE_NUMBER + " = ?");
            whereArgs.add(employeeNumber);
            hasWhere = true;
        }
        if (startDate != null && !startDate.isEmpty()) {
            queryBuilder.append(hasWhere ? " AND " : " WHERE ").append("hl." + COLUMN_WASH_DATE + " >= ?");
            whereArgs.add(startDate);
            hasWhere = true;
        }
        if (endDate != null && !endDate.isEmpty()) {
            queryBuilder.append(hasWhere ? " AND " : " WHERE ").append("hl." + COLUMN_WASH_DATE + " <= ?");
            whereArgs.add(endDate);
        }
        queryBuilder.append(" ORDER BY hl." + COLUMN_WASH_DATE + " DESC, hl." + COLUMN_WASH_TIME + " DESC");

        Cursor cursor = null;
        try {
            Log.d(TAG, "SearchHandwashLogs Query: " + queryBuilder.toString());
            cursor = db.rawQuery(queryBuilder.toString(), whereArgs.toArray(new String[0]));

            if (cursor.moveToFirst()) {
                do {
                    HandwashLog log = new HandwashLog();
                    log.employeeNumber = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMPLOYEE_NUMBER));
                    log.washDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WASH_DATE));
                    log.washTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WASH_TIME));
                    log.photoPath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHOTO_PATH));
                    log.firstName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRST_NAME));
                    log.lastName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_NAME));
                    logs.add(log);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error searching handwash logs: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return logs;
    }


    public long insertEmployee(String employeeNumber, String firstName, String lastName, String department) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EMPLOYEE_NUMBER, employeeNumber);
        values.put(COLUMN_FIRST_NAME, firstName);
        values.put(COLUMN_LAST_NAME, lastName);
        values.put(COLUMN_DEPARTMENT, department);
        values.put(COLUMN_IS_ACTIVE, 1);

        long id = -1;
        try {
            id = db.insertWithOnConflict(TABLE_EMPLOYEES, null, values, SQLiteDatabase.CONFLICT_IGNORE);
            if (id == -1) {
                Log.w(TAG, "Insert ignored for employee number (likely exists): " + employeeNumber);
                Cursor existingCursor = null;
                try {
                    existingCursor = db.query(TABLE_EMPLOYEES, new String[]{COLUMN_ID}, COLUMN_EMPLOYEE_NUMBER + "=?", new String[]{employeeNumber}, null, null, null);
                    if(existingCursor.moveToFirst()){
                        id = existingCursor.getLong(existingCursor.getColumnIndexOrThrow(COLUMN_ID));
                    }
                } finally {
                    if (existingCursor != null) existingCursor.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error inserting employee: " + e.getMessage());
        } finally {
            db.close();
        }
        return id;
    }


    public List<HandwashLog> getHandwashLogs(String startDate, String endDate, String downloadType) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<HandwashLog> logs = new ArrayList<>();
        String query;
        ArrayList<String> selectionArgs = new ArrayList<>();

        if (downloadType.equals("summary")) {
            query = "SELECT hl." + COLUMN_EMPLOYEE_NUMBER + ", e." + COLUMN_FIRST_NAME + ", e." + COLUMN_LAST_NAME +
                    ", COUNT(hl." + COLUMN_EMPLOYEE_NUMBER + ") as total_washes " +
                    "FROM " + TABLE_HANDWASH_LOG + " hl " +
                    "LEFT JOIN " + TABLE_EMPLOYEES + " e ON hl." + COLUMN_EMPLOYEE_NUMBER + " = e." + COLUMN_EMPLOYEE_NUMBER;
            if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
                query += " WHERE hl." + COLUMN_WASH_DATE + " BETWEEN ? AND ?";
                selectionArgs.add(startDate);
                selectionArgs.add(endDate);
            } else if (startDate != null && !startDate.isEmpty()) {
                query += " WHERE hl." + COLUMN_WASH_DATE + " >= ?";
                selectionArgs.add(startDate);
            } else if (endDate != null && !endDate.isEmpty()) {
                query += " WHERE hl." + COLUMN_WASH_DATE + " <= ?";
                selectionArgs.add(endDate);
            }
            query += " GROUP BY hl." + COLUMN_EMPLOYEE_NUMBER + ", e." + COLUMN_FIRST_NAME + ", e." + COLUMN_LAST_NAME +
                    " ORDER BY total_washes DESC";
        } else {
            query = "SELECT hl.*, e." + COLUMN_FIRST_NAME + ", e." + COLUMN_LAST_NAME +
                    " FROM " + TABLE_HANDWASH_LOG + " hl " +
                    "LEFT JOIN " + TABLE_EMPLOYEES + " e ON hl." + COLUMN_EMPLOYEE_NUMBER + " = e." + COLUMN_EMPLOYEE_NUMBER;
            if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
                query += " WHERE hl." + COLUMN_WASH_DATE + " BETWEEN ? AND ?";
                selectionArgs.add(startDate);
                selectionArgs.add(endDate);
            } else if (startDate != null && !startDate.isEmpty()) {
                query += " WHERE hl." + COLUMN_WASH_DATE + " >= ?";
                selectionArgs.add(startDate);
            } else if (endDate != null && !endDate.isEmpty()) {
                query += " WHERE hl." + COLUMN_WASH_DATE + " <= ?";
                selectionArgs.add(endDate);
            }
            query += " ORDER BY hl." + COLUMN_WASH_DATE + " DESC, hl." + COLUMN_WASH_TIME + " DESC";
        }
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, selectionArgs.toArray(new String[0]));
            if (cursor.moveToFirst()) {
                do {
                    HandwashLog log = new HandwashLog();
                    log.employeeNumber = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMPLOYEE_NUMBER));
                    if (downloadType.equals("summary")) {
                        log.washCount = cursor.getInt(cursor.getColumnIndexOrThrow("total_washes"));
                        log.firstName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRST_NAME));
                        log.lastName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_NAME));
                    } else {
                        log.washDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WASH_DATE));
                        log.washTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WASH_TIME));
                        log.photoPath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHOTO_PATH));
                        log.firstName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRST_NAME));
                        log.lastName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_NAME));
                    }
                    logs.add(log);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting handwash logs for download: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return logs;
    }

    public static class HandwashLog {
        public String employeeNumber;
        public String washDate;
        public String washTime;
        public String photoPath;
        public int washCount;
        public String firstName;
        public String lastName;
    }


    public List<LeaderboardEntry> getTopHandwashers() {
        SQLiteDatabase db = this.getReadableDatabase(); //
        List<LeaderboardEntry> leaderboard = new ArrayList<>(); //
        LocalDate currentDate = LocalDate.now(); //
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault()); //
        String today = currentDate.format(formatter); //
        Cursor cursor = null;
        try {
            // MODIFIED QUERY to include last_name
            String query = "SELECT e." + COLUMN_EMPLOYEE_NUMBER + ", e." + COLUMN_FIRST_NAME + ", e." + COLUMN_LAST_NAME + // Added COLUMN_LAST_NAME
                    ", COUNT(hl." + COLUMN_ID + ") AS handwash_count " +
                    "FROM " + TABLE_EMPLOYEES + " e JOIN " + TABLE_HANDWASH_LOG + " hl " +
                    "ON e." + COLUMN_EMPLOYEE_NUMBER + " = hl." + COLUMN_EMPLOYEE_NUMBER +
                    " WHERE hl." + COLUMN_WASH_DATE + " = ? AND e." + COLUMN_IS_ACTIVE + " = 1" +
                    " GROUP BY e." + COLUMN_EMPLOYEE_NUMBER + ", e." + COLUMN_FIRST_NAME + ", e." + COLUMN_LAST_NAME + // Added COLUMN_LAST_NAME to GROUP BY
                    " ORDER BY handwash_count DESC LIMIT 5";
            cursor = db.rawQuery(query, new String[]{today}); //
            if (cursor.moveToFirst()) { //
                do {
                    String empNumber = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMPLOYEE_NUMBER)); //
                    String firstName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRST_NAME)); //
                    String lastNameValue = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_NAME)); // NEW: Get lastName
                    int count = cursor.getInt(cursor.getColumnIndexOrThrow("handwash_count")); //
                    // MODIFIED Instantiation
                    leaderboard.add(new LeaderboardEntry(empNumber, firstName, lastNameValue, count)); //
                } while (cursor.moveToNext());
            }
        } catch (Exception e) { //
            Log.e(TAG, "Error getting top handwashers: " + e.getMessage()); //
        } finally {
            if (cursor != null) cursor.close(); //
            db.close(); //
        }
        return leaderboard; //
    }


    public long insertHandwashLog(String employeeNumber, String washDate, String washTime, String photoPath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EMPLOYEE_NUMBER, employeeNumber);
        values.put(COLUMN_WASH_DATE, washDate);
        values.put(COLUMN_WASH_TIME, washTime);
        values.put(COLUMN_PHOTO_PATH, photoPath);
        long id = -1;
        try {
            id = db.insertOrThrow(TABLE_HANDWASH_LOG, null, values);
        } catch (Exception e){
            Log.e(TAG, "Error inserting handwash log: " + e.getMessage());
        } finally {
            db.close();
        }
        return id;
    }

    public boolean updateAdminPassword(String username, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        values.put(COLUMN_PASSWORD_HASH, hashedPassword);
        int rowsAffected = 0;
        try {
            rowsAffected = db.update(TABLE_ADMIN_USERS, values, COLUMN_USERNAME + " = ?", new String[]{username});
        } catch (Exception e){
            Log.e(TAG, "Error updating admin password: " + e.getMessage());
        } finally {
            db.close();
        }
        return rowsAffected > 0;
    }

    public boolean validateAdminLogin(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_ADMIN_USERS, new String[]{COLUMN_PASSWORD_HASH},
                    COLUMN_USERNAME + " = ?", new String[]{username}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                String storedHash = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD_HASH));
                return BCrypt.checkpw(password, storedHash);
            }
        } catch (Exception e){
            Log.e(TAG, "Error validating admin login: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return false;
    }

    public int deleteHandwashLogs(String startDate, String endDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        String whereClause = COLUMN_WASH_DATE + " BETWEEN ? AND ?";
        String[] whereArgs = new String[]{startDate, endDate};
        int rowsAffected = 0;
        try {
            rowsAffected = db.delete(TABLE_HANDWASH_LOG, whereClause, whereArgs);
        } catch (Exception e) {
            Log.e(TAG, "Error deleting handwash logs: " + e.getMessage());
        } finally {
            db.close();
        }
        return rowsAffected;
    }

    public boolean doesEmployeeExist(String employeeNumber) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        boolean exists = false;
        try {
            cursor = db.query(TABLE_EMPLOYEES, new String[]{COLUMN_ID},
                    COLUMN_EMPLOYEE_NUMBER + " = ? AND " + COLUMN_IS_ACTIVE + " = 1",
                    new String[]{employeeNumber}, null, null, null);
            if (cursor != null) {
                exists = (cursor.getCount() > 0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking if employee exists: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return exists;
    }

    public int getHandwashCountForEmployeeToday(String employeeNumber) {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());
        String today = currentDate.format(formatter);
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_HANDWASH_LOG +
                            " WHERE " + COLUMN_EMPLOYEE_NUMBER + " = ? AND " +
                            COLUMN_WASH_DATE + " = ?",
                    new String[]{employeeNumber, today});
            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting handwash count for employee today: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return count;
    }
}
