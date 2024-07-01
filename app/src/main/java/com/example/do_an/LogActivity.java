package com.example.do_an;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class LogActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ScanLogAdapter adapter;
    private DatabaseHelper databaseHelper;
    private List<ScanLogEntry> scanLogEntries;
    private Button clearLogButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        databaseHelper = new DatabaseHelper(this);
        scanLogEntries = getScanLogEntriesFromDatabase();

        adapter = new ScanLogAdapter(scanLogEntries, this);
        recyclerView.setAdapter(adapter);

        clearLogButton = findViewById(R.id.clearLogButton);
        clearLogButton.setOnClickListener(v -> clearLogs());
    }

    private List<ScanLogEntry> getScanLogEntriesFromDatabase() {
        List<ScanLogEntry> entries = new ArrayList<>();
        Cursor cursor = databaseHelper.getAllScanResults();

        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") String time = cursor.getString(cursor.getColumnIndex("time"));
                @SuppressLint("Range") String fileName = cursor.getString(cursor.getColumnIndex("file_name"));
                @SuppressLint("Range") String status = cursor.getString(cursor.getColumnIndex("result"));
                @SuppressLint("Range") String link = cursor.getString(cursor.getColumnIndex("link"));
                entries.add(new ScanLogEntry(time, fileName, status, link));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return entries;
    }

    private void clearLogs() {
        databaseHelper.deleteAllScanResults();
        scanLogEntries.clear();
        adapter.notifyDataSetChanged();
        Toast.makeText(this, "All logs cleared", Toast.LENGTH_SHORT).show();
    }
}
