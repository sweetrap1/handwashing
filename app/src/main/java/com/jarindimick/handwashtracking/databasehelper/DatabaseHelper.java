package com.jarindimick.handwashtracking.databasehelper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Handwash.db";
    private static final int DATABASE_VERSION = 1;

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
    public static final String COLUMN_PASSWORD = "password";
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
                + COLUMN_PASSWORD + " TEXT,"
                + COLUMN_ROLE + " TEXT" + ")";
        db.execSQL(CREATE_ADMIN_USERS_TABLE);

        // Insert initial admin user (for testing - REPLACE WITH SECURE METHOD LATER)
        String INSERT_ADMIN = "INSERT INTO " + TABLE_ADMIN_USERS + "(" + COLUMN_USERNAME + ", " + COLUMN_PASSWORD + ", " + COLUMN_ROLE + ") VALUES ('admin', 'Maddox8545!', 'administrator')";
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

    public long insertEmployee(String employeeNumber) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EMPLOYEE_NUMBER, employeeNumber);
        values.put(COLUMN_FIRST_NAME, ""); // Default empty (can be updated later)
        values.put(COLUMN_LAST_NAME, "");  // Default empty (can be updated later)
        values.put(COLUMN_IS_ACTIVE, 1);    // Default active

        long id = db.insert(TABLE_EMPLOYEES, null, values);
        db.close();
        return id; // Returns the row ID of the newly inserted row, or -1 if an error occurred
    }
}