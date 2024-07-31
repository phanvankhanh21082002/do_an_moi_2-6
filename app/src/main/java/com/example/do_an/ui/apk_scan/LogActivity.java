package com.example.do_an.ui.apk_scan;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an.R;

import java.util.ArrayList;
import java.util.List;

public class LogActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ScanLogAdapter adapter;
    private DatabaseHelper databaseHelper;
    private List<ScanLogEntry> scanLogEntries;
    private Button clearLogButton;
    private boolean launchedFromDrawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        databaseHelper = new DatabaseHelper(this);
        scanLogEntries = getScanLogEntriesFromDatabase();

        adapter = new ScanLogAdapter(scanLogEntries, this);
        recyclerView.setAdapter(adapter);

        clearLogButton = findViewById(R.id.clearLogButton);
        clearLogButton.setOnClickListener(v -> clearLogs());

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (launchedFromDrawer) {
                DrawerLayout drawer = findViewById(R.id.drawer_layout);
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                } else {
                    drawer.openDrawer(GravityCompat.START);
                }
            } else {
                Intent intent = new Intent(this, ScanAPK.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);

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
