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


import com.example.do_an.R;
import com.github.angads25.filepicker.view.FilePickerDialog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ScanAPK extends AppCompatActivity {
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private Executor executor = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());
    private OkHttpClient client = new OkHttpClient();
    Button fileSelectorButton;
    TextView selectedFileTextView;
    Button uploadButton;
    ProgressDialog uploadScanDialog;
    ProgressDialog downloadScanDialog;
    TextView scanCompleteTextView;
    Button downloadButton;
    TextView downloadedTextView;
    DatabaseHelper databaseHelper;
    FilePickerDialog dialog;
    String fileName=null;

    private ImageView startScan;
    private ImageView scanningProcess;
    private ImageView scanCompleted;
    private ImageView scanFailed;
    private TextView letScan;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_apk_scan);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE},
                PackageManager.PERMISSION_GRANTED);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fileSelectorButton = (Button) findViewById(R.id.fileSelectorButton);
        selectedFileTextView = (TextView) findViewById(R.id.selectedFileTextView);
        uploadButton = (Button) findViewById(R.id.uploadButton);
        scanCompleteTextView = (TextView) findViewById(R.id.scanCompleteTextView);
        letScan = (TextView) findViewById(R.id.text_notify1);

        startScan = (ImageView) findViewById(R.id.log_start_scan);
        scanningProcess = (ImageView) findViewById(R.id.logo_scanning);
        scanCompleted = (ImageView) findViewById(R.id.logo_scan_completed);
        scanFailed = (ImageView) findViewById(R.id.logo_scan_failed);

        databaseHelper = new DatabaseHelper(this);

        setupFilePickerLauncher();
        fileSelectorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickFile();
                startScan.setVisibility(View.VISIBLE);
                scanFailed.setVisibility(View.GONE);
                scanningProcess.setVisibility(View.GONE);
                scanCompleted.setVisibility(View.GONE);
            }
        });


    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
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
                        letScan.setVisibility(View.GONE);
                        selectedFileTextView.setVisibility(View.VISIBLE);
                        selectedFileTextView.setText("Selected file: " + fileName);
                        uploadButton.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                startScan.setVisibility(View.GONE);
                                scanFailed.setVisibility(View.GONE);
                                scanningProcess.setVisibility(View.VISIBLE);
                                uploadFile(fileUri);
                            }
                        });
                    }
                });
    }

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(intent);
    }

    private void uploadFile(Uri fileUri) {
        // Create and show a ProgressDialog
        ProgressDialog dialog = new ProgressDialog(ScanAPK.this);
        dialog.setMessage("Scanning file...");
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.show();

        new Thread(() -> {
            try {
                fileName = getFileName(fileUri);
                RequestBody fileBody = UploadFileToServer.createRequestBodyFromUri(this, fileUri);
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("uploaded_file", fileName, fileBody)
                        .build();

                Request request = new Request.Builder()
                        .url("http://192.168.1.139:3000/upload_file")
                        .post(requestBody)
                        .build();

                OkHttpClient client = new OkHttpClient();
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(() -> {
                            dialog.dismiss();
                            scanningProcess.setVisibility(View.GONE);
                            scanFailed.setVisibility(View.VISIBLE);
                            showToast("Scan failed: " + e.getMessage());
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            runOnUiThread(() -> {
                                checkForReport(fileName, dialog);
                            });
                        } else {
                            runOnUiThread(() -> {
                                dialog.dismiss();

                                showToast("Scan failed with code: " + response.code());
                            });
                        }
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    dialog.dismiss();
                    showToast("Error preparing upload: " + e.getMessage());
                });
            }
        }).start();
    }

    private void checkForReport(String fileName, ProgressDialog dialog) {
        executor.execute(() -> {
            boolean reportReady = false;
            while (!reportReady) {
                try {
                    URL url = new URL("http://192.168.1.139/reports/" + fileName + ".txt");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        reportReady = true; // Set flag to true to break the loop

                        handler.post(() -> {
                            dialog.dismiss();
                            showToast("Report is ready!");
                            scanningProcess.setVisibility(View.GONE);
                            scanCompleted.setVisibility(View.VISIBLE);
                            try {
                                displayReportContent(connection); // Optional, to display report content
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    } else {
                        Thread.sleep(4000);  // Check again after 4 seconds
                    }
                } catch (Exception e) {
                    handler.post(() -> {
                        dialog.dismiss(); // Dismiss dialog in case of an error
                        showToast("Error checking report: " + e.getMessage());
                    });
                    break; // Exit the loop in case of an error
                }
            }
        });
    }

    private void displayReportContent(HttpURLConnection connection) throws IOException {
        // Read and display the content of the report
        InputStream inputStream = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line + "\n");
        }
        reader.close();
        String reportContent = stringBuilder.toString();
        System.out.println(reportContent);
        // Save the result to the database
        if (databaseHelper == null) {
            databaseHelper = new DatabaseHelper(this);
        }
        databaseHelper.insertScanResult(fileName,reportContent);
        handler.post(() -> {
            // Display the content in a TextView or other UI element
            scanCompleteTextView.setText(reportContent);
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


}
