package com.example.do_an.ui.apk_scan;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.do_an.R;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ScanWorker extends Worker {
    private static final String CHANNEL_ID = "scan_results";
    private DatabaseHelper databaseHelper;

    public ScanWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        databaseHelper = new DatabaseHelper(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        String fileName = getInputData().getString("fileName");
        String fileHash = getInputData().getString("fileHash");

        // Perform the file scan (upload and check for report)
        boolean success = performScan(fileName, fileHash);
        return success ? Result.success() : Result.failure();
    }

    private boolean performScan(String fileName, String fileHash) {
        try {
            boolean reportReady = false;
            while (!reportReady) {
                URL url = new URL("http://35.247.145.117/reports_txt/" + fileHash + ".txt");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    reportReady = true;
                    InputStream inputStream = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder reportContent = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        reportContent.append(line).append("\n");
                    }
                    reader.close();

                    String status = determineStatus(reportContent.toString());
                    saveScanResult(fileName, fileHash, status);
                    showNotification(fileName, status);
                } else {
                    Thread.sleep(4000);
                }
            }
            return true;
        } catch (Exception e) {
            Log.e("ScanWorker", "Error performing scan", e);
            return false;
        }
    }

    private String determineStatus(String reportContent) {
        if (reportContent.contains("Result: Warning")) {
            return "Warning";
        } else if (reportContent.contains("Result: Malware")) {
            return "Malware";
        } else {
            return "Clean";
        }
    }

    private void saveScanResult(String fileName, String fileHash, String status) {
        String link = "http://35.247.145.117/reports_html/" + fileHash + ".html";
        // Check if the result already exists in the database
        Cursor cursor = databaseHelper.getScanResultByFileHash(fileHash);
        if (cursor.moveToFirst()) {
            // Update the existing result
            databaseHelper.updateScanResultTime(fileHash);
        } else {
            // Insert new result
            databaseHelper.insertScanResult(fileName, fileHash, status, link);
        }
        cursor.close();
        databaseHelper.removeDuplicateLogs();
    }

    @SuppressLint("MissingPermission")
    private void showNotification(String fileName, String status) {
        createNotificationChannel();

        Intent intent = new Intent(getApplicationContext(), ScanAPK.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("fromNotification", true);
        intent.putExtra("fileName", fileName);
        intent.putExtra("status", status);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Scan Result for: " + fileName)
                .setContentText("Status: " + status)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.notification_color));

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        notificationManager.notify(1, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Scan Results";
            String description = "Channel for scan result notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getApplicationContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}

