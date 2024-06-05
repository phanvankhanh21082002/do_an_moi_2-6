package com.example.do_an.ui.apk_scan;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.do_an.R;

public class ShowDatabaseActivity extends AppCompatActivity {

    TextView tvContent;
    DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_database);

        tvContent = findViewById(R.id.tvContent);
        databaseHelper = new DatabaseHelper(this);

        showDatabaseContent();
    }

    private void showDatabaseContent() {
        StringBuilder content = new StringBuilder();
        Cursor cursor = databaseHelper.getAllScanResults();

        if (cursor != null) {
            while (cursor.moveToNext()) {
                @SuppressLint("Range") String fileName = cursor.getString(cursor.getColumnIndex("file_name"));
                @SuppressLint("Range") String time = cursor.getString(cursor.getColumnIndex("time"));
                @SuppressLint("Range") String result = cursor.getString(cursor.getColumnIndex("result"));
                content.append("File Name: ").append(fileName).append("\n");
                content.append("Time: ").append(time).append("\n");
                content.append("Result: ").append(result).append("\n\n");
            }
            cursor.close();
        }
        tvContent.setText(content.toString());
    }
}
