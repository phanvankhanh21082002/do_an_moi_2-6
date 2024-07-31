package com.example.do_an.ui.apk_scan;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkManager;

import com.example.do_an.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;


public class ScanAPK extends AppCompatActivity {
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final OkHttpClient client = new OkHttpClient();
    Button fileSelectorButton;
    TextView selectedFileTextView;
    TextView scanCompleteTextView;
    Button viewDetailsButton;
    DatabaseHelper databaseHelper;
    String fileName = null;
    String fileHash = null;
    private ImageView startScan;
    private ImageView scanningProcess;
    private ImageView scanCompleted;
    private ImageView scanFailed;
    private String status;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apk_scan);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE},
                PackageManager.PERMISSION_GRANTED);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fileSelectorButton = findViewById(R.id.fileSelectorButton);
        selectedFileTextView = findViewById(R.id.selectedFileTextView);
        scanCompleteTextView = findViewById(R.id.scanCompleteTextView);
        viewDetailsButton = findViewById(R.id.detailsButton);

        startScan = findViewById(R.id.log_start_scan);
        scanningProcess = findViewById(R.id.logo_scanning);
        scanCompleted = findViewById(R.id.logo_scan_completed);
        scanFailed = findViewById(R.id.logo_scan_failed);
        Button logButton = findViewById(R.id.logButton);

        databaseHelper = new DatabaseHelper(this);

        setupFilePickerLauncher();
        fileSelectorButton.setOnClickListener(v -> pickFile());
        logButton.setOnClickListener(v -> {
            Intent intent = new Intent(ScanAPK.this, LogActivity.class);
            startActivity(intent);
        });
        startScan.setVisibility(View.VISIBLE);
        scanCompleteTextView.setVisibility(View.GONE);
        scanFailed.setVisibility(View.GONE);
        scanningProcess.setVisibility(View.GONE);
        scanCompleted.setVisibility(View.GONE);

        if (getIntent().getBooleanExtra("fromNotification", false)) {
            fileName = getIntent().getStringExtra("fileName");
            status = getIntent().getStringExtra("status");

            if (fileName != null && status != null) {
                displayScanResult(fileName, status);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupFilePickerLauncher() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri fileUri = result.getData().getData();
                        fileName = getFileName(fileUri);
                        fileHash = calculateFileHash(fileUri);
                        scanCompleted.setVisibility(View.GONE);
                        selectedFileTextView.setVisibility(View.VISIBLE);
                        selectedFileTextView.setText("Selected file: " + fileName);
                        checkAndUploadFile(fileUri);
                    }
                });
    }

    private void pickFile() {
        Toast.makeText(this, "Please select an APK file only.", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/vnd.android.package-archive");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        filePickerLauncher.launch(intent);
    }

    private void checkAndUploadFile(Uri fileUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            if (inputStream != null) {
                int fileSize = inputStream.available();
                inputStream.close();
                if (fileSize > 200 * 1024 * 1024) {
                    runOnUiThread(() -> {
                        selectedFileTextView.setVisibility(View.GONE);
                        showToast("File too large. Please select a file smaller than 30MB.");
                    });
                    return;
                }
            }
        } catch (IOException e) {
            runOnUiThread(() -> {
                showToast("Error checking file size: " + e.getMessage());
            });
            return;
        }
        scanCompleted.setVisibility(View.GONE);
        Cursor cursor = databaseHelper.getScanResultByFileHash(fileHash);
        if (cursor.moveToFirst()) {
            @SuppressLint("Range") String existingFileName = cursor.getString(cursor.getColumnIndex("file_name"));
            @SuppressLint("Range") String existingResult = cursor.getString(cursor.getColumnIndex("result"));
            @SuppressLint("Range") String existingLink = cursor.getString(cursor.getColumnIndex("link"));

            scanCompleteTextView.setVisibility(View.GONE);
            databaseHelper.updateScanResultTime(fileHash);
            displayExistingResult(existingFileName, existingResult, existingLink);
        } else {
            uploadFile(fileUri);
        }
        cursor.close();
    }

    private void uploadFile(Uri fileUri) {
        ProgressDialog dialog = new ProgressDialog(ScanAPK.this);
        dialog.setMessage("Uploading file...");
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.dismiss();

        startScan.setVisibility(View.GONE);
        scanFailed.setVisibility(View.GONE);
        scanCompleted.setVisibility(View.GONE);
        scanCompleteTextView.setVisibility(View.GONE);
        scanningProcess.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                fileName = getFileName(fileUri);
                fileHash = calculateFileHash(fileUri);
                String filePath = UploadFileToServer.saveUriToFile(this, fileUri); // Save the file locally

                // Prepare data for WorkManager
                Data inputData = new Data.Builder()
                        .putString("filePath", filePath)
                        .putString("fileHash", fileHash)
                        .putString("fileName", fileName)
                        .build();

                // Create WorkRequests
                OneTimeWorkRequest uploadWorkRequest = new OneTimeWorkRequest.Builder(UploadWorker.class)
                        .setInputData(inputData)
                        .build();

                OneTimeWorkRequest scanWorkRequest = new OneTimeWorkRequest.Builder(ScanWorker.class)
                        .setInputData(inputData)
                        .build();

                // Chain WorkRequests
                WorkContinuation continuation = WorkManager.getInstance(this)
                        .beginWith(uploadWorkRequest)
                        .then(scanWorkRequest);

                continuation.enqueue();

                handler.post(() -> {
                    WorkManager.getInstance(this).getWorkInfoByIdLiveData(scanWorkRequest.getId())
                            .observe(this, workInfo -> {
                                if (workInfo != null && workInfo.getState().isFinished()) {
                                    checkForReport(fileHash, dialog);
                                }
                            });
                });

                handler.post(() -> {
                    dialog.dismiss();
                    Toast.makeText(ScanAPK.this, "Upload initiated. You will be notified upon completion.", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    dialog.dismiss();
                    showToast("Error preparing upload: " + e.getMessage());
                    Log.e("UploadFileToServer", "Failed to prepare upload", e);
                });
            }
        }).start();
    }

    private void checkForReport(String fileHash, ProgressDialog dialog) {
        executor.execute(() -> {
            boolean reportReady = false;
            while (!reportReady) {
                try {
                    URL url = new URL("http://35.247.145.117/reports_txt/" + fileHash + ".txt");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        reportReady = true;
                        handler.post(() -> {
                            dialog.dismiss();
                            showToast("Report is ready!");
                            scanningProcess.setVisibility(View.GONE);
                            scanCompleted.setVisibility(View.VISIBLE);
                            try {
                                displayReportContent(connection, fileHash);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    } else {
                        Thread.sleep(4000);
                    }
                } catch (Exception e) {
                    handler.post(() -> {
                        dialog.dismiss();
                        showToast("Error checking report: " + e.getMessage());
                        Log.e("checkForReport", "Failed to check report", e);
                    });
                    break;
                }
            }
        });

        // Trigger WorkManager to handle the background task when the app is closed
        Data data = new Data.Builder()
                .putString("fileName", fileName)
                .putString("fileHash", fileHash)
                .build();

        OneTimeWorkRequest scanWorkRequest = new OneTimeWorkRequest.Builder(ScanWorker.class)
                .setInputData(data)
                .build();

        WorkManager.getInstance(this).enqueue(scanWorkRequest);
    }

    private void displayReportContent(HttpURLConnection connection, String fileHash) throws IOException {
        InputStream inputStream = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder reportContent = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            reportContent.append(line).append("\n");
        }
        reader.close();

        String status;
        if (reportContent.toString().contains("Result: Warning")) {
            status = "Warning";
        } else if (reportContent.toString().contains("Result: Malware")) {
            status = "Malware";
        } else {
            status = "Clean";
        }

        handler.post(() -> {
            // Check if the result already exists in the database
            Cursor cursor = databaseHelper.getScanResultByFileHash(fileHash);
            if (cursor.moveToFirst()) {
                // Update the existing result
                databaseHelper.updateScanResultTime(fileHash);
            } else {
                // Insert new result
                String link = "http://35.247.145.117/reports_html/" + fileHash + ".html";
                databaseHelper.insertScanResult(fileName, fileHash, status, link);
            }
            cursor.close();
            databaseHelper.removeDuplicateLogs();  // Remove duplicate logs
            displayScanResult(fileName, status);
        });
    }

    private void displayScanResult(String fileName, String status) {
        selectedFileTextView.setVisibility(View.VISIBLE);
        selectedFileTextView.setText("Selected file: " + fileName);

        scanCompleteTextView.setVisibility(View.VISIBLE);
        scanCompleteTextView.setText(status);

        if (status.equals("Warning")) {
            scanCompleteTextView.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
        } else if (status.equals("Malware")) {
            scanCompleteTextView.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
        } else if (status.equals("Clean")) {
            scanCompleteTextView.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
        }

        viewDetailsButton.setVisibility(View.VISIBLE);
        viewDetailsButton.setOnClickListener(v -> {
            String link = "http://35.247.145.117/reports_html/" + fileHash + ".html";
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
            startActivity(browserIntent);
        });
    }

    private void displayExistingResult(String fileName, String status, String link) {
        handler.post(() -> {
            selectedFileTextView.setVisibility(View.VISIBLE);
            selectedFileTextView.setText("Selected file: " + fileName);

            scanCompleteTextView.setVisibility(View.VISIBLE);
            scanCompleteTextView.setText(status);

            if (status.equals("Warning")) {
                scanCompleteTextView.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
            } else if (status.equals("Malware")) {
                scanCompleteTextView.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
            } else if (status.equals("Clean")) {
                scanCompleteTextView.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
            }

            viewDetailsButton.setVisibility(View.VISIBLE);
            viewDetailsButton.setOnClickListener(v -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                startActivity(browserIntent);
            });
        });
    }

    private void showToast(String message) {
        Toast.makeText(ScanAPK.this, message, Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("Range")
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } catch (Exception e) {
                Log.e("getFileName", "Failed to get file name", e);
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private String calculateFileHash(Uri fileUri) {
        try (InputStream inputStream = getContentResolver().openInputStream(fileUri)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            byte[] hashBytes = digest.digest();
            StringBuilder hashString = new StringBuilder();
            for (byte b : hashBytes) {
                hashString.append(String.format("%02x", b));
            }
            return hashString.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to calculate file hash", e);
        }
    }
}



