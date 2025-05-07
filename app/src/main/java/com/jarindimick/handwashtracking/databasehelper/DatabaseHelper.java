package com.jarindimick.handwashtracking.databasehelper;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;

import com.jarindimick.handwashtracking.gui.LeaderboardEntry;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.mindrot.jbcrypt.BCrypt; // Import BCrypt

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Handwash.db";
    private static final int DATABASE_VERSION = 2; // Increment version if you change the schema

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
    public static final String COLUMN_IS_ACTIVE = "is_active";

    // Handwash Log table column names
    public static final String COLUMN_WASH_DATE = "wash_date";
    public static final String COLUMN_WASH_TIME = "wash_time";
    public static final String COLUMN_PHOTO_PATH = "photo_path";

    // Admin Users table column names
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_PASSWORD_HASH = "password_hash"; // Changed: Store hash, not plain text
    public static final String COLUMN_ROLE = "role";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Employees table
        String CREATE_EMPLOYEES_TABLE = "CREATE TABLE " + TABLE_EMPLOYEES + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_EMPLOYEE_NUMBER + " TEXT UNIQUE,"
                + COLUMN_FIRST_NAME + " TEXT,"
                + COLUMN_LAST_NAME + " TEXT,"
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
                + COLUMN_PASSWORD_HASH + " TEXT," // Changed: Store hash
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

    public long insertEmployee(String employeeNumber, String firstName, String lastName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EMPLOYEE_NUMBER, employeeNumber);
        values.put(COLUMN_FIRST_NAME, firstName);
        values.put(COLUMN_LAST_NAME, lastName);
        values.put(COLUMN_IS_ACTIVE, 1);    // Default active

        // First, check if the employee number already exists
        Cursor cursor = db.query(TABLE_EMPLOYEES, new String[]{COLUMN_ID}, COLUMN_EMPLOYEE_NUMBER + "=?", new String[]{employeeNumber}, null, null, null);
        long id;
        if (cursor.getCount() > 0) {
            // Employee exists, so update (or do nothing, depending on your needs)
            // Here, we'll assume you want to update the 'is_active' status or similar
            // values.put(COLUMN_IS_ACTIVE, 1); // Ensure they are marked as active
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
        return id; // Returns the row ID of the newly inserted row, or -1 if an error occurred
    }

    public long insertEmployee(String employeeNumber) {
        return insertEmployee(employeeNumber, "", "");
    }

    public List<LeaderboardEntry> getTopHandwashers() {
        SQLiteDatabase db = this.getReadableDatabase();
        List<LeaderboardEntry> leaderboard = new ArrayList<>();

        // Get the current date in YYYY-MM-DD format
        java.time.LocalDate currentDate = java.time.LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String today = currentDate.format(formatter);

        // SQL to get the top 5 employees by handwash count for the current day
        String query = "SELECT " + TABLE_EMPLOYEES + "." + COLUMN_EMPLOYEE_NUMBER + ", "
                + COLUMN_FIRST_NAME + ", "
                + "COUNT(" + TABLE_HANDWASH_LOG + "." + COLUMN_EMPLOYEE_NUMBER + ") AS handwash_count "
                + "FROM " + TABLE_EMPLOYEES + " INNER JOIN " + TABLE_HANDWASH_LOG  // Use INNER JOIN to only include employees with washes
                + " ON " + TABLE_EMPLOYEES + "." + COLUMN_EMPLOYEE_NUMBER + " = " + TABLE_HANDWASH_LOG + "." + COLUMN_EMPLOYEE_NUMBER
                + " WHERE " + TABLE_HANDWASH_LOG + "." + COLUMN_WASH_DATE + " = '" + today + "' "  // Filter by current date
                + " GROUP BY " + TABLE_EMPLOYEES + "." + COLUMN_EMPLOYEE_NUMBER + ", " + COLUMN_FIRST_NAME
                + " ORDER BY handwash_count DESC LIMIT 5";

        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                String employeeNumber = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMPLOYEE_NUMBER));
                String firstName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRST_NAME));
                int handwashCount = cursor.getInt(cursor.getColumnIndexOrThrow("handwash_count"));
                leaderboard.add(new LeaderboardEntry(employeeNumber, firstName, handwashCount));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return leaderboard;
    }
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


    public boolean updateAdminPassword(String username, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt()); // Hash the new password
        values.put(COLUMN_PASSWORD_HASH, hashedPassword);
        int rowsAffected = db.update(TABLE_ADMIN_USERS, values, COLUMN_USERNAME + " = ?", new String[]{username});
        db.close();
        return rowsAffected > 0; // Returns true if the update was successful (at least one row was updated)
    }
    // New method to validate admin login
    public boolean validateAdminLogin(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_PASSWORD_HASH + " FROM " + TABLE_ADMIN_USERS +
                " WHERE " + COLUMN_USERNAME + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{username});
        if (cursor.moveToFirst()) {
            String storedHash = cursor.getString(0);
            cursor.close();
            db.close();
            return BCrypt.checkpw(password, storedHash); // Compare entered password with stored hash
        }
        cursor.close();
        db.close();
        return false; // User not found or password doesn't match
    }

    public int deleteHandwashLogs(String startDate, String endDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        String whereClause = COLUMN_WASH_DATE + " BETWEEN ? AND ?";
        String[] whereArgs = new String[]{startDate, endDate};

        int rowsDeleted = db.delete(TABLE_HANDWASH_LOG, whereClause, whereArgs);
        db.close();
        return rowsDeleted;
    }
}