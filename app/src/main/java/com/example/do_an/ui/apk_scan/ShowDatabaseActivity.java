package com.example.do_an.ui.apk_scan;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.do_an.R;

public class ShowDatabaseActivity extends AppCompatActivity {

    TextView tvContent;
    DatabaseHelper databaseHelper;
    Button deleteDatabasebutton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_database);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        tvContent = findViewById(R.id.tvContent);
        databaseHelper = new DatabaseHelper(this);
        deleteDatabasebutton = findViewById(R.id.deleteDatabase);

        showDatabaseContent();
        deleteDatabasebutton.setOnClickListener(v -> confirmAndDeleteAllResults());

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
                String resultText = "File Name: " + fileName + "\n" +
                        "Time: " + time + "\n" +
                        "Result: " + result + "\n\n";
                content.insert(0, resultText); // Prepend the result to the content
            }
            cursor.close();
        }
        tvContent.setText(content.toString());
    }

    private void confirmAndDeleteAllResults() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete all results?")
                .setPositiveButton("Yes", (dialog, which) -> deleteAllScanResultsAndUpdateUI())
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteAllScanResultsAndUpdateUI() {
        databaseHelper.deleteAllScanResults();
        tvContent.setText(""); // Clear the content of TextView to reflect that the database is empty
    }
}
