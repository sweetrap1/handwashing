package com.jarindimick.handwashtracking.databasehelper;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;

import com.jarindimick.handwashtracking.gui.LeaderboardEntry;

import java.time.LocalDate; // Import LocalDate
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale; // Import Locale

import org.mindrot.jbcrypt.BCrypt;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Handwash.db";
    private static final int DATABASE_VERSION = 3; // Keep the database version at 3 from previous changes

    // Table names
    public static final String TABLE_EMPLOYEES = "employees";
    public static final String TABLE_HANDWASH_LOG = "handwash_log";
    public static final String TABLE_ADMIN_USERS = "admin_users";

    // Common column names
    public static final String COLUMN_ID = "id";

    // Employees table column names
    public static final String COLUMN_EMPLOYEE_NUMBER = "employee_number";
    public static final String COLUMN_FIRST_NAME = "first_name";
    public static final String COLUMN_LAST_NAME = "last_name";
    public static final String COLUMN_DEPARTMENT = "department";
    public static final String COLUMN_IS_ACTIVE = "is_active";

    // Handwash Log table column names
    public static final String COLUMN_WASH_DATE = "wash_date";
    public static final String COLUMN_WASH_TIME = "wash_time";
    public static final String COLUMN_PHOTO_PATH = "photo_path";

    // Admin Users table column names
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_PASSWORD_HASH = "password_hash";
    public static final String COLUMN_ROLE = "role";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public List<HandwashLog> searchHandwashLogs(String firstName, String lastName, String employeeNumber, String startDate, String endDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<HandwashLog> logs = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_HANDWASH_LOG;
        String whereClause = "";
        List<String> whereArgs = new ArrayList<>();

        // Build the WHERE clause dynamically
        if (firstName != null && !firstName.isEmpty()) {
            if (!whereClause.isEmpty()) whereClause += " AND ";
            whereClause += COLUMN_EMPLOYEE_NUMBER + " IN (SELECT " + COLUMN_EMPLOYEE_NUMBER + " FROM " + TABLE_EMPLOYEES + " WHERE " + COLUMN_FIRST_NAME + " LIKE ?)";
            whereArgs.add("%" + firstName + "%");
        }
        if (lastName != null && !lastName.isEmpty()) {
            if (!whereClause.isEmpty()) whereClause += " AND ";
            whereClause += COLUMN_EMPLOYEE_NUMBER + " IN (SELECT " + COLUMN_EMPLOYEE_NUMBER + " FROM " + TABLE_EMPLOYEES + " WHERE " + COLUMN_LAST_NAME + " LIKE ?)";
            whereArgs.add("%" + lastName + "%");
        }
        if (employeeNumber != null && !employeeNumber.isEmpty()) {
            if (!whereClause.isEmpty()) whereClause += " AND ";
            whereClause += COLUMN_EMPLOYEE_NUMBER + " = ?";
            whereArgs.add(employeeNumber);
        }
        if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
            if (!whereClause.isEmpty()) whereClause += " AND ";
            whereClause += COLUMN_WASH_DATE + " BETWEEN ? AND ?";
            whereArgs.add(startDate);
            whereArgs.add(endDate);
        } else if (startDate != null && !startDate.isEmpty()) {
            if (!whereClause.isEmpty()) whereClause += " AND ";
            whereClause += COLUMN_WASH_DATE + " >= ?";
            whereArgs.add(startDate);
        } else if (endDate != null && !endDate.isEmpty()) {
            if (!whereClause.isEmpty()) whereClause += " AND ";
            whereClause += COLUMN_WASH_DATE + " <= ?";
            whereArgs.add(endDate);
        }

        if (!whereClause.isEmpty()) {
            query += " WHERE " + whereClause;
        }

        Cursor cursor = db.rawQuery(query, whereArgs.toArray(new String[0]));

        if (cursor.moveToFirst()) {
            do {
                HandwashLog log = new HandwashLog();
                log.employeeNumber = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMPLOYEE_NUMBER));
                log.washDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WASH_DATE));
                log.washTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WASH_TIME));
                log.photoPath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHOTO_PATH));
                logs.add(log);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return logs;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Employees table
        String CREATE_EMPLOYEES_TABLE = "CREATE TABLE " + TABLE_EMPLOYEES + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_EMPLOYEE_NUMBER + " TEXT UNIQUE,"
                + COLUMN_FIRST_NAME + " TEXT,"
                + COLUMN_LAST_NAME + " TEXT,"
                + COLUMN_DEPARTMENT + " TEXT,"
                + COLUMN_IS_ACTIVE + " INTEGER" + ")";
        db.execSQL(CREATE_EMPLOYEES_TABLE);

        // Create Handwash Log table
        String CREATE_HANDWASH_LOG_TABLE = "CREATE TABLE " + TABLE_HANDWASH_LOG + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_EMPLOYEE_NUMBER + " TEXT,"
                + COLUMN_WASH_DATE + " TEXT,"
                + COLUMN_WASH_TIME + " TEXT,"
                + COLUMN_PHOTO_PATH + " TEXT" + ")";
        db.execSQL(CREATE_HANDWASH_LOG_TABLE);

        // Create Admin Users table
        String CREATE_ADMIN_USERS_TABLE = "CREATE TABLE " + TABLE_ADMIN_USERS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_USERNAME + " TEXT UNIQUE,"
                + COLUMN_PASSWORD_HASH + " TEXT,"
                + COLUMN_ROLE + " TEXT" + ")";
        db.execSQL(CREATE_ADMIN_USERS_TABLE);

        // Insert initial admin user (SECURELY with BCrypt)
        String hashedPassword = BCrypt.hashpw("Maddox8545!", BCrypt.gensalt());
        String INSERT_ADMIN = "INSERT INTO " + TABLE_ADMIN_USERS + "(" + COLUMN_USERNAME + ", " + COLUMN_PASSWORD_HASH + ", " + COLUMN_ROLE + ") VALUES ('admin', '" + hashedPassword + "', 'administrator')";
        db.execSQL(INSERT_ADMIN);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older tables if they exist
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EMPLOYEES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HANDWASH_LOG);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ADMIN_USERS);

        // Create tables again
        onCreate(db);
    }

    /**
     * Inserts a new employee into the employees table.
     *
     * @param employeeNumber The employee's number.
     * @param firstName      The employee's first name.
     * @param lastName       The employee's last name.
     * @param department     The employee's department.
     * @return The row ID of the newly inserted row, or -1 if an error occurred.
     */
    public long insertEmployee(String employeeNumber, String firstName, String lastName, String department) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EMPLOYEE_NUMBER, employeeNumber);
        values.put(COLUMN_FIRST_NAME, firstName);
        values.put(COLUMN_LAST_NAME, lastName);
        values.put(COLUMN_DEPARTMENT, department);
        values.put(COLUMN_IS_ACTIVE, 1);    // Default active

        // First, check if the employee number already exists
        Cursor cursor = db.query(TABLE_EMPLOYEES, new String[]{COLUMN_ID}, COLUMN_EMPLOYEE_NUMBER + "=?", new String[]{employeeNumber}, null, null, null);
        long id;
        if (cursor.getCount() > 0) {
            // Employee exists, so update
            id = db.update(TABLE_EMPLOYEES, values, COLUMN_EMPLOYEE_NUMBER + "=?", new String[]{employeeNumber});
            if (id == 0) {
                id = -1; // Indicate error during update
            }
        } else {
            // Employee doesn't exist, so insert
            id = db.insert(TABLE_EMPLOYEES, null, values);
        }
        cursor.close();
        db.close();
        return id;
    }

    /**
     * Overloaded method to insert a new employee with only the employee number.
     * First name, last name, and department are set to empty strings.
     *
     * @param employeeNumber The employee's number.
     * @return The row ID of the newly inserted row, or -1 if an error occurred.
     */
    public long insertEmployee(String employeeNumber) {
        return insertEmployee(employeeNumber, "", "", "");
    }

    /**
     * Retrieves handwash logs based on the specified date range and download type.
     *
     * @param startDate    The start date for the log range (YYYY-MM-DD).
     * @param endDate      The end date for the log range (YYYY-MM-DD).
     * @param downloadType The type of download ("summary" or "detailed").
     * @return A List of HandwashLog objects containing the retrieved data.
     */
    public List<HandwashLog> getHandwashLogs(String startDate, String endDate, String downloadType) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<HandwashLog> logs = new ArrayList<>();
        String query;

        if (downloadType.equals("summary")) {
            // Summary: Employee number and total handwash count
            query = "SELECT " + COLUMN_EMPLOYEE_NUMBER + ", COUNT(*) as total_washes " +
                    "FROM " + TABLE_HANDWASH_LOG +
                    " WHERE " + COLUMN_WASH_DATE + " BETWEEN ? AND ? " +
                    "GROUP BY " + COLUMN_EMPLOYEE_NUMBER;
        } else {
            // Detailed: All columns from handwash_log
            query = "SELECT * FROM " + TABLE_HANDWASH_LOG +
                    " WHERE " + COLUMN_WASH_DATE + " BETWEEN ? AND ?";
        }

        Cursor cursor = db.rawQuery(query, new String[]{startDate, endDate});

        if (cursor.moveToFirst()) {
            do {
                HandwashLog log = new HandwashLog();
                log.employeeNumber = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMPLOYEE_NUMBER));
                if (downloadType.equals("summary")) {
                    log.washCount = cursor.getInt(cursor.getColumnIndexOrThrow("total_washes"));
                } else {
                    log.washDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WASH_DATE));
                    log.washTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WASH_TIME));
                    log.photoPath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHOTO_PATH));
                }
                logs.add(log);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return logs;
    }

    /**
     * Helper class to represent HandwashLog data.
     */
    public static class HandwashLog {
        public String employeeNumber;
        public String washDate;
        public String washTime;
        public String photoPath;
        public int washCount; // For summary data
    }

    /**
     * Retrieves the top 5 employees with the most handwashes for the current day.
     *
     * @return A List of LeaderboardEntry objects representing the top handwashers.
     */
    public List<LeaderboardEntry> getTopHandwashers() {
        SQLiteDatabase db = this.getReadableDatabase();
        List<LeaderboardEntry> leaderboard = new ArrayList<>();

        // Get the current date in yyyy-MM-DD format
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String today = currentDate.format(formatter);

        // SQL to get the top 5 employees by handwash count for the current day
        String query = "SELECT " + TABLE_EMPLOYEES + "." + COLUMN_EMPLOYEE_NUMBER + ", "
                + COLUMN_FIRST_NAME + ", " // Retrieving first name
                + "COUNT(" + TABLE_HANDWASH_LOG + "." + COLUMN_EMPLOYEE_NUMBER + ") AS handwash_count "
                + "FROM " + TABLE_EMPLOYEES + " INNER JOIN " + TABLE_HANDWASH_LOG
                + " ON " + TABLE_EMPLOYEES + "." + COLUMN_EMPLOYEE_NUMBER + " = " + TABLE_HANDWASH_LOG + "." + COLUMN_EMPLOYEE_NUMBER
                + " WHERE " + TABLE_HANDWASH_LOG + "." + COLUMN_WASH_DATE + " = '" + today + "' "
                + " GROUP BY " + TABLE_EMPLOYEES + "." + COLUMN_EMPLOYEE_NUMBER + ", " + COLUMN_FIRST_NAME
                + " ORDER BY handwash_count DESC LIMIT 5";

        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                String employeeNumber = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMPLOYEE_NUMBER));
                String firstName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRST_NAME)); // Get first name
                int handwashCount = cursor.getInt(cursor.getColumnIndexOrThrow("handwash_count"));
                leaderboard.add(new LeaderboardEntry(employeeNumber, firstName, handwashCount)); // Pass first name
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return leaderboard;
    }

    /**
     * Inserts a new handwash log entry.
     *
     * @param employeeNumber The employee's number.
     * @param washDate       The date of the handwash (YYYY-MM-DD).
     * @param washTime       The time of the handwash (HH:MM:SS).
     * @param photoPath      The path to the photo (if any).
     * @return The row ID of the newly inserted row, or -1 if an error occurred.
     */
    public long insertHandwashLog(String employeeNumber, String washDate, String washTime, String photoPath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EMPLOYEE_NUMBER, employeeNumber);
        values.put(COLUMN_WASH_DATE, washDate);
        values.put(COLUMN_WASH_TIME, washTime);
        values.put(COLUMN_PHOTO_PATH, photoPath);

        long id = db.insert(TABLE_HANDWASH_LOG, null, values);
        db.close();
        return id;
    }

    /**
     * Updates the admin password in the admin_users table.
     *
     * @param username    The admin's username.
     * @param newPassword The new password (will be hashed).
     * @return True if the update was successful, false otherwise.
     */
    public boolean updateAdminPassword(String username, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        values.put(COLUMN_PASSWORD_HASH, hashedPassword);
        int rowsAffected = db.update(TABLE_ADMIN_USERS, values, COLUMN_USERNAME + " = ?", new String[]{username});
        db.close();
        return rowsAffected > 0;
    }

    /**
     * Validates the admin login credentials.
     *
     * @param username The admin's username.
     * @param password The admin's password.
     * @return True if the credentials are valid, false otherwise.
     */
    public boolean validateAdminLogin(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_PASSWORD_HASH + " FROM " + TABLE_ADMIN_USERS +
                " WHERE " + COLUMN_USERNAME + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{username});
        if (cursor.moveToFirst()) {
            String storedHash = cursor.getString(0);
            cursor.close();
            db.close();
            return BCrypt.checkpw(password, storedHash);
        }
        cursor.close();
        db.close();
        return false;
    }

    /**
     * Deletes handwash logs within the specified date range.
     *
     * @param startDate The start date (YYYY-MM-DD).
     * @param endDate   The end date (YYYY-MM-DD).
     * @return The number of rows deleted.
     */
    public int deleteHandwashLogs(String startDate, String endDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        String whereClause = COLUMN_WASH_DATE + " BETWEEN ? AND ?";
        String[] whereArgs = new String[]{startDate, endDate};

        int rowsDeleted = db.delete(TABLE_HANDWASH_LOG, whereClause, whereArgs);
        db.close();
        return rowsDeleted;
    }

    /**
     * Checks if an employee with the given employee number exists in the database.
     * @param employeeNumber The employee number to check.
     * @return True if the employee exists, false otherwise.
     */
    public boolean doesEmployeeExist(String employeeNumber) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_ID + " FROM " + TABLE_EMPLOYEES +
                " WHERE " + COLUMN_EMPLOYEE_NUMBER + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{employeeNumber});
        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        db.close();
        return exists;
    }

    /**
     * Retrieves the number of handwashes for a specific employee for the current day.
     * @param employeeNumber The employee number.
     * @return The count of handwashes for the employee today.
     */
    public int getHandwashCountForEmployeeToday(String employeeNumber) {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;

        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String today = currentDate.format(formatter);

        String query = "SELECT COUNT(*) FROM " + TABLE_HANDWASH_LOG +
                " WHERE " + COLUMN_EMPLOYEE_NUMBER + " = ? AND " +
                COLUMN_WASH_DATE + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{employeeNumber, today});

        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }
}