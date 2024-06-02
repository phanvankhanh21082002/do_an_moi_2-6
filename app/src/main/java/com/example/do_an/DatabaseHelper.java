package com.example.do_an;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "fileScan.db";
    private static final int DATABASE_VERSION = 3;

    private static final String TABLE_NAME = "scan_results";
    private static final String COLUMN_FILE_NAME = "file_name";
    private static final String COLUMN_TIME = "time";
    private static final String COLUMN_RESULT = "result";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_FILE_NAME + " TEXT, " +
                COLUMN_TIME + " TEXT, " +
                COLUMN_RESULT + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            // Trong trường hợp cần thiết hơn, bạn có thể sử dụng ALTER TABLE để chỉnh sửa cấu trúc
            // Hiện tại, chúng ta chỉ xóa và tạo lại bảng để đơn giản
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }

    public boolean insertScanResult(String fileName, String result) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_FILE_NAME, fileName);
        contentValues.put(COLUMN_TIME, getCurrentTime());
        contentValues.put(COLUMN_RESULT, result);
        long resultId = db.insert(TABLE_NAME, null, contentValues);
        return resultId != -1;
    }

    public Cursor getAllScanResults() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_NAME, null, null, null, null, null, null);
    }

    public void deleteAllScanResults() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_NAME);
    }

    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }
}
