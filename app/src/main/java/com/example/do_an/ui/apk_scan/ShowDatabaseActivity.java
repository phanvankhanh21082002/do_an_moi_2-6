package com.example.do_an.ui.apk_scan;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.do_an.R;

public class ShowDatabaseActivity extends AppCompatActivity {

    TextView tvContent;
    DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_database);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        tvContent = findViewById(R.id.tvContent);
        databaseHelper = new DatabaseHelper(this);
        showDatabaseContent();


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle back arrow click here
        if (item.getItemId() == android.R.id.home) {
            // Finish the activity and return to ScanAPK
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
