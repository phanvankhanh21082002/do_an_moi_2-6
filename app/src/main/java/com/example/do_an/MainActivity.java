//package com.example.do_an;
//import android.annotation.SuppressLint;
//import android.app.Activity;
//import android.app.ProgressDialog;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.database.Cursor;
//import android.net.Uri;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Looper;
//import android.provider.OpenableColumns;
//import android.util.Log;
//import android.view.View;
//import android.widget.Button;
//import android.widget.TextView;
//import android.widget.Toast;
//import android.Manifest;
//import androidx.activity.result.ActivityResultLauncher;
//import androidx.activity.result.contract.ActivityResultContracts;
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//import com.github.angads25.filepicker.view.FilePickerDialog;
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
//import java.util.concurrent.Executor;
//import java.util.concurrent.Executors;
//
//import okhttp3.Call;
//import okhttp3.Callback;
//import okhttp3.MultipartBody;
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.RequestBody;
//import okhttp3.Response;
//public class MainActivity extends AppCompatActivity {
//    private ActivityResultLauncher<Intent> filePickerLauncher;
//    private Executor executor = Executors.newSingleThreadExecutor();
//    private Handler handler = new Handler(Looper.getMainLooper());
//    private OkHttpClient client = new OkHttpClient();
//    Button fileSelectorButton;
//    TextView selectedFileTextView;
//    Button uploadButton;
//    ProgressDialog uploadScanDialog;
//    ProgressDialog downloadScanDialog;
//    TextView scanCompleteTextView;
//    Button downloadButton;
//    TextView downloadedTextView;
//    Button showDatabaseButton;
//    DatabaseHelper databaseHelper;
//    FilePickerDialog dialog;
//    String fileName = null;
//    String fileHash = null;
//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        ActivityCompat.requestPermissions(this,
//                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                        Manifest.permission.READ_EXTERNAL_STORAGE},
//                PackageManager.PERMISSION_GRANTED);
//
//        fileSelectorButton = findViewById(R.id.fileSelectorButton);
//        selectedFileTextView = findViewById(R.id.selectedFileTextView);
//        uploadButton = findViewById(R.id.uploadButton);
//        scanCompleteTextView = findViewById(R.id.scanCompleteTextView);
//        showDatabaseButton = findViewById(R.id.showDatabaseButton);
//        databaseHelper = new DatabaseHelper(this);
//
//        setupFilePickerLauncher();
//        fileSelectorButton.setOnClickListener(v -> pickFile());
//
//        showDatabaseButton.setOnClickListener(v -> {
//            Intent intent = new Intent(MainActivity.this, ShowDatabaseActivity.class);
//            startActivity(intent);
//        });
//    }
//
//    private void setupFilePickerLauncher() {
//        filePickerLauncher = registerForActivityResult(
//                new ActivityResultContracts.StartActivityForResult(),
//                result -> {
//                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
//                        Uri fileUri = result.getData().getData();
//                        fileName = getFileName(fileUri);
//                        fileHash = calculateFileHash(fileUri);
//                        selectedFileTextView.setVisibility(View.VISIBLE);
//                        selectedFileTextView.setText(fileName);
//                        uploadButton.setOnClickListener(v -> uploadFile(fileUri));
//                    }
//                });
//    }
//
//    private void pickFile() {
//        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//        intent.setType("*/*");
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        filePickerLauncher.launch(intent);
//    }
//
//    private void uploadFile(Uri fileUri) {
//        ProgressDialog dialog = new ProgressDialog(MainActivity.this);
//        dialog.setMessage("Uploading file...");
//        dialog.setIndeterminate(true);
//        dialog.setCancelable(false);
//        dialog.show();
//
//        new Thread(() -> {
//            try {
//                fileName = getFileName(fileUri);
//                fileHash = calculateFileHash(fileUri);
//                RequestBody fileBody = UploadFileToServer.createRequestBodyFromUri(this, fileUri);
//                RequestBody requestBody = new MultipartBody.Builder()
//                        .setType(MultipartBody.FORM)
//                        .addFormDataPart("uploaded_file", fileName, fileBody)
//                        .addFormDataPart("file_hash", fileHash)
//                        .build();
//
//                Request request = new Request.Builder()
//                        .url("http://10.10.10.42:3000/upload_file")
//                        .post(requestBody)
//                        .build();
//
//                OkHttpClient client = new OkHttpClient();
//                client.newCall(request).enqueue(new Callback() {
//                    @Override
//                    public void onFailure(Call call, IOException e) {
//                        runOnUiThread(() -> {
//                            dialog.dismiss();
//                            showToast("Upload failed: " + e.getMessage());
//                        });
//                    }
//
//                    @Override
//                    public void onResponse(Call call, Response response) throws IOException {
//                        if (response.isSuccessful()) {
//                            runOnUiThread(() -> checkForReport(fileHash, dialog));
//                        } else {
//                            runOnUiThread(() -> {
//                                dialog.dismiss();
//                                showToast("Upload failed with code: " + response.code());
//                            });
//                        }
//                    }
//                });
//            } catch (Exception e) {
//                runOnUiThread(() -> {
//                    dialog.dismiss();
//                    showToast("Error preparing upload: " + e.getMessage());
//                });
//            }
//        }).start();
//    }
//
//    private void checkForReport(String fileHash, ProgressDialog dialog) {
//        executor.execute(() -> {
//            boolean reportReady = false;
//            while (!reportReady) {
//                try {
//                    URL url = new URL("http://10.10.10.42/reports_txt/" + fileHash + ".txt");
//                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//                    connection.setRequestMethod("GET");
//                    int responseCode = connection.getResponseCode();
//                    if (responseCode == HttpURLConnection.HTTP_OK) {
//                        reportReady = true; // Set flag to true to break the loop
//                        handler.post(() -> {
//                            dialog.dismiss();
//                            showToast("Report is ready!");
//                            try {
//                                displayReportContent(connection, fileHash); // Pass the file hash to displayReportContent
//                            } catch (IOException e) {
//                                throw new RuntimeException(e);
//                            }
//                        });
//                    } else {
//                        Thread.sleep(4000);  // Check again after 4 seconds
//                    }
//                } catch (Exception e) {
//                    handler.post(() -> {
//                        dialog.dismiss(); // Dismiss dialog in case of an error
//                        showToast("Error checking report: " + e.getMessage());
//                    });
//                    break; // Exit the loop in case of an error
//                }
//            }
//        });
//    }
//
//    private void displayReportContent(HttpURLConnection connection, String fileHash) throws IOException {
//        InputStream inputStream = connection.getInputStream();
//        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
//        StringBuilder stringBuilder = new StringBuilder();
//        String line;
//        while ((line = reader.readLine()) != null) {
//            stringBuilder.append(line).append("<br>");
//        }
//        reader.close();
//        String reportContent = stringBuilder.toString();
//        System.out.println(reportContent);
//        if (databaseHelper == null) {
//            databaseHelper = new DatabaseHelper(this);
//        }
//        databaseHelper.insertScanResult(fileName, reportContent);
//        handler.post(() -> {
//            String linkText = "<a href=\"http://10.10.10.42/reports_html/" + fileHash + ".html\">" + fileHash + "</a>";
//            scanCompleteTextView.setVisibility(View.VISIBLE);
//            scanCompleteTextView.setText(android.text.Html.fromHtml(reportContent + "<br><br>Chi tiết file ở đây: " + linkText));
//            scanCompleteTextView.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
//        });
//    }
//
//    private void showToast(String message) {
//        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
//    }
//
//    @SuppressLint("Range")
//    private String getFileName(Uri uri) {
//        String result = null;
//        if (uri.getScheme().equals("content")) {
//            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
//                if (cursor != null && cursor.moveToFirst()) {
//                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
//                }
//            } catch (Exception e) {
//                Log.e("getFileName", "Failed to get file name", e);
//            }
//        }
//        if (result == null) {
//            result = uri.getPath();
//            int cut = result.lastIndexOf('/');
//            if (cut != -1) {
//                result = result.substring(cut + 1);
//            }
//        }
//        return result;
//    }
//
//    private String calculateFileHash(Uri fileUri) {
//        try (InputStream inputStream = getContentResolver().openInputStream(fileUri)) {
//            MessageDigest digest = MessageDigest.getInstance("SHA-256");
//            byte[] buffer = new byte[8192];
//            int bytesRead;
//            while ((bytesRead = inputStream.read(buffer)) != -1) {
//                digest.update(buffer, 0, bytesRead);
//            }
//            byte[] hashBytes = digest.digest();
//            StringBuilder hashString = new StringBuilder();
//            for (byte b : hashBytes) {
//                hashString.append(String.format("%02x", b));
//            }
//            return hashString.toString();
//        } catch (IOException | NoSuchAlgorithmException e) {
//            throw new RuntimeException("Failed to calculate file hash", e);
//        }
//    }
//}
//

//package com.example.do_an;
//
//import android.annotation.SuppressLint;
//import android.app.Activity;
//import android.app.ProgressDialog;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.database.Cursor;
//import android.graphics.drawable.Drawable;
//import android.net.Uri;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Looper;
//import android.provider.OpenableColumns;
//import android.util.Log;
//import android.view.View;
//import android.widget.Button;
//import android.widget.ImageView;
//import android.widget.TextView;
//import android.widget.Toast;
//import android.Manifest;
//import androidx.activity.result.ActivityResultLauncher;
//import androidx.activity.result.contract.ActivityResultContracts;
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//import com.github.angads25.filepicker.view.FilePickerDialog;
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
//import java.util.concurrent.Executor;
//import java.util.concurrent.Executors;
//
//import okhttp3.Call;
//import okhttp3.Callback;
//import okhttp3.MultipartBody;
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.RequestBody;
//import okhttp3.Response;
//
//public class MainActivity extends AppCompatActivity {
//    private ActivityResultLauncher<Intent> filePickerLauncher;
//    private Executor executor = Executors.newSingleThreadExecutor();
//    private Handler handler = new Handler(Looper.getMainLooper());
//    private OkHttpClient client = new OkHttpClient();
//    Button fileSelectorButton;
//    TextView selectedFileTextView;
//    Button uploadButton;
//    ProgressDialog uploadScanDialog;
//    ProgressDialog downloadScanDialog;
//    TextView scanCompleteTextView;
//    Button viewDetailsButton;
//    ImageView apkImageView;
//    DatabaseHelper databaseHelper;
//    FilePickerDialog dialog;
//    String fileName = null;
//    String fileHash = null;
//
//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        ActivityCompat.requestPermissions(this,
//                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                        Manifest.permission.READ_EXTERNAL_STORAGE},
//                PackageManager.PERMISSION_GRANTED);
//
//        fileSelectorButton = findViewById(R.id.fileSelectorButton);
//        selectedFileTextView = findViewById(R.id.selectedFileTextView);
//        uploadButton = findViewById(R.id.uploadButton);
//        scanCompleteTextView = findViewById(R.id.scanCompleteTextView);
//        viewDetailsButton = findViewById(R.id.detailsButton);
//        apkImageView = findViewById(R.id.apkImageView);
//        Button logButton = findViewById(R.id.logButton);
//        databaseHelper = new DatabaseHelper(this);
//
//        setupFilePickerLauncher();
//        fileSelectorButton.setOnClickListener(v -> pickFile());
//        logButton.setOnClickListener(v -> {
//            Intent intent = new Intent(MainActivity.this, LogActivity.class);
//            startActivity(intent);
//        });
//    }
//
//    private void setupFilePickerLauncher() {
//        filePickerLauncher = registerForActivityResult(
//                new ActivityResultContracts.StartActivityForResult(),
//                result -> {
//                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
//                        Uri fileUri = result.getData().getData();
//                        fileName = getFileName(fileUri);
//                        fileHash = calculateFileHash(fileUri);
//                        selectedFileTextView.setVisibility(View.VISIBLE);
//                        scanCompleteTextView.setVisibility(View.GONE);
//                        selectedFileTextView.setText(fileName);
//
//                        try {
//                            Drawable apkIcon = getPackageManager().getApplicationIcon(fileUri.getPath());
//                            apkImageView.setImageDrawable(apkIcon);
//                        } catch (PackageManager.NameNotFoundException e) {
//                            apkImageView.setImageResource(R.drawable.ic_launcher_background);
//                        }
//
//                        uploadButton.setOnClickListener(v -> uploadFile(fileUri));
//                    }
//                });
//    }
//
//    private void pickFile() {
//        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//        intent.setType("*/*");
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        filePickerLauncher.launch(intent);
//    }
//
//    private void uploadFile(Uri fileUri) {
//        ProgressDialog dialog = new ProgressDialog(MainActivity.this);
//        dialog.setMessage("Uploading file...");
//        dialog.setIndeterminate(true);
//        dialog.setCancelable(false);
//        dialog.show();
//
//        new Thread(() -> {
//            try {
//                fileName = getFileName(fileUri);
//                fileHash = calculateFileHash(fileUri);
//                RequestBody fileBody = UploadFileToServer.createRequestBodyFromUri(this, fileUri);
//                RequestBody requestBody = new MultipartBody.Builder()
//                        .setType(MultipartBody.FORM)
//                        .addFormDataPart("uploaded_file", fileName, fileBody)
//                        .addFormDataPart("file_hash", fileHash)
//                        .build();
//
//                Request request = new Request.Builder()
//                        .url("http://10.10.10.45:3000/upload_file")
//                        .post(requestBody)
//                        .build();
//
//                OkHttpClient client = new OkHttpClient();
//                client.newCall(request).enqueue(new Callback() {
//                    @Override
//                    public void onFailure(Call call, IOException e) {
//                        runOnUiThread(() -> {
//                            dialog.dismiss();
//                            showToast("Upload failed: " + e.getMessage());
//                        });
//                    }
//
//                    @Override
//                    public void onResponse(Call call, Response response) throws IOException {
//                        if (response.isSuccessful()) {
//                            runOnUiThread(() -> checkForReport(fileHash, dialog));
//                        } else {
//                            runOnUiThread(() -> {
//                                dialog.dismiss();
//                                showToast("Upload failed with code: " + response.code());
//                            });
//                        }
//                    }
//                });
//            } catch (Exception e) {
//                runOnUiThread(() -> {
//                    dialog.dismiss();
//                    showToast("Error preparing upload: " + e.getMessage());
//                });
//            }
//        }).start();
//    }
//
//    private void checkForReport(String fileHash, ProgressDialog dialog) {
//        executor.execute(() -> {
//            boolean reportReady = false;
//            while (!reportReady) {
//                try {
//                    URL url = new URL("http://10.10.10.45/reports_txt/" + fileHash + ".txt");
//                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//                    connection.setRequestMethod("GET");
//                    int responseCode = connection.getResponseCode();
//                    if (responseCode == HttpURLConnection.HTTP_OK) {
//                        reportReady = true;
//                        handler.post(() -> {
//                            dialog.dismiss();
//                            showToast("Report is ready!");
//                            try {
//                                displayReportContent(connection, fileHash);
//                            } catch (IOException e) {
//                                throw new RuntimeException(e);
//                            }
//                        });
//                    } else {
//                        Thread.sleep(4000);
//                    }
//                } catch (Exception e) {
//                    handler.post(() -> {
//                        dialog.dismiss();
//                        showToast("Error checking report: " + e.getMessage());
//                    });
//                    break;
//                }
//            }
//        });
//    }
//
//    private void displayReportContent(HttpURLConnection connection, String fileHash) throws IOException {
//        InputStream inputStream = connection.getInputStream();
//        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
//        StringBuilder stringBuilder = new StringBuilder();
//        String line;
//        while ((line = reader.readLine()) != null) {
//            stringBuilder.append(line).append("<br>");
//        }
//        reader.close();
//        String reportContent = stringBuilder.toString();
//        System.out.println(reportContent);
//
//        handler.post(() -> {
//            String resultText = reportContent;
//            String status = "";
//            if (resultText.contains("Result: Warning")) {
//                scanCompleteTextView.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
//                status = "Warning";
//            } else if (resultText.contains("Result: Malware")) {
//                scanCompleteTextView.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
//                status = "Malware";
//            } else if (resultText.contains("Result: Clean")) {
//                scanCompleteTextView.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
//                status = "Clean";
//            }
//
//            String link = "http://10.10.10.45/reports_html/" + fileHash + ".html";
//            databaseHelper.insertScanResult(fileName, fileHash, status, link);
//
//            scanCompleteTextView.setVisibility(View.VISIBLE);
//            scanCompleteTextView.setText(status);
//            scanCompleteTextView.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
//            viewDetailsButton.setVisibility(View.VISIBLE);
//            viewDetailsButton.setOnClickListener(v -> {
//                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
//                startActivity(browserIntent);
//            });
//        });
//    }
//
//    private void showToast(String message) {
//        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
//    }
//
//    @SuppressLint("Range")
//    private String getFileName(Uri uri) {
//        String result = null;
//        if (uri.getScheme().equals("content")) {
//            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
//                if (cursor != null && cursor.moveToFirst()) {
//                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
//                }
//            } catch (Exception e) {
//                Log.e("getFileName", "Failed to get file name", e);
//            }
//        }
//        if (result == null) {
//            result = uri.getPath();
//            int cut = result.lastIndexOf('/');
//            if (cut != -1) {
//                result = result.substring(cut + 1);
//            }
//        }
//        return result;
//    }
//
//    private String calculateFileHash(Uri fileUri) {
//        try (InputStream inputStream = getContentResolver().openInputStream(fileUri)) {
//            MessageDigest digest = MessageDigest.getInstance("SHA-256");
//            byte[] buffer = new byte[8192];
//            int bytesRead;
//            while ((bytesRead = inputStream.read(buffer)) != -1) {
//                digest.update(buffer, 0, bytesRead);
//            }
//            byte[] hashBytes = digest.digest();
//            StringBuilder hashString = new StringBuilder();
//            for (byte b : hashBytes) {
//                hashString.append(String.format("%02x", b));
//            }
//            return hashString.toString();
//        } catch (IOException | NoSuchAlgorithmException e) {
//            throw new RuntimeException("Failed to calculate file hash", e);
//        }
//    }
//}

package com.example.do_an;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.github.angads25.filepicker.view.FilePickerDialog;
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

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
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
    Button viewDetailsButton;
    ImageView apkImageView;
    DatabaseHelper databaseHelper;
    FilePickerDialog dialog;
    String fileName = null;
    String fileHash = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE},
                PackageManager.PERMISSION_GRANTED);

        fileSelectorButton = findViewById(R.id.fileSelectorButton);
        selectedFileTextView = findViewById(R.id.selectedFileTextView);
        uploadButton = findViewById(R.id.uploadButton);
        scanCompleteTextView = findViewById(R.id.scanCompleteTextView);
        viewDetailsButton = findViewById(R.id.detailsButton);
        apkImageView = findViewById(R.id.apkImageView);
        Button logButton = findViewById(R.id.logButton);
        databaseHelper = new DatabaseHelper(this);

        setupFilePickerLauncher();
        fileSelectorButton.setOnClickListener(v -> pickFile());
        logButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LogActivity.class);
            startActivity(intent);
        });
    }

    private void setupFilePickerLauncher() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri fileUri = result.getData().getData();
                        fileName = getFileName(fileUri);
                        fileHash = calculateFileHash(fileUri);
                        selectedFileTextView.setVisibility(View.VISIBLE);
                        scanCompleteTextView.setVisibility(View.GONE);
                        selectedFileTextView.setText(fileName);

                        try {
                            Drawable apkIcon = getPackageManager().getApplicationIcon(fileUri.getPath());
                            apkImageView.setImageDrawable(apkIcon);
                        } catch (PackageManager.NameNotFoundException e) {
                            apkImageView.setImageResource(R.drawable.ic_launcher_background);
                        }

                        checkAndUploadFile(fileUri);
                    }
                });
    }

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(intent);
    }

    private void checkAndUploadFile(Uri fileUri) {
        // Check if the file hash exists in the database
        Cursor cursor = databaseHelper.getScanResultByFileHash(fileHash);
        if (cursor.moveToFirst()) {
            @SuppressLint("Range") String existingTime = cursor.getString(cursor.getColumnIndex("time"));
            @SuppressLint("Range") String existingFileName = cursor.getString(cursor.getColumnIndex("file_name"));
            @SuppressLint("Range") String existingResult = cursor.getString(cursor.getColumnIndex("result"));
            @SuppressLint("Range") String existingLink = cursor.getString(cursor.getColumnIndex("link"));

            // Update the time in the database
            databaseHelper.updateScanResultTime(fileHash);

            // Display the existing result
            displayExistingResult(existingFileName, existingResult, existingLink);
            
        } else {
            // Upload and scan the file if the hash doesn't exist in the database
            uploadFile(fileUri);
        }
        cursor.close();
    }

    private void uploadFile(Uri fileUri) {
        ProgressDialog dialog = new ProgressDialog(MainActivity.this);
        dialog.setMessage("Uploading file...");
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.show();

        new Thread(() -> {
            try {
                fileName = getFileName(fileUri);
                fileHash = calculateFileHash(fileUri);
                RequestBody fileBody = UploadFileToServer.createRequestBodyFromUri(this, fileUri);
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("uploaded_file", fileName, fileBody)
                        .addFormDataPart("file_hash", fileHash)
                        .build();

                Request request = new Request.Builder()
                        .url("http://10.10.10.45:3000/upload_file")
                        .post(requestBody)
                        .build();

                OkHttpClient client = new OkHttpClient();
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(() -> {
                            dialog.dismiss();
                            showToast("Upload failed: " + e.getMessage());
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            runOnUiThread(() -> checkForReport(fileHash, dialog));
                        } else {
                            runOnUiThread(() -> {
                                dialog.dismiss();
                                showToast("Upload failed with code: " + response.code());
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

    private void checkForReport(String fileHash, ProgressDialog dialog) {
        executor.execute(() -> {
            boolean reportReady = false;
            while (!reportReady) {
                try {
                    URL url = new URL("http://10.10.10.45/reports_txt/" + fileHash + ".txt");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        reportReady = true;
                        handler.post(() -> {
                            dialog.dismiss();
                            showToast("Report is ready!");
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
                    });
                    break;
                }
            }
        });
    }

    private void displayReportContent(HttpURLConnection connection, String fileHash) throws IOException {
        InputStream inputStream = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line).append("<br>");
        }
        reader.close();
        String reportContent = stringBuilder.toString();
        System.out.println(reportContent);

        handler.post(() -> {
            String resultText = reportContent;
            String status = "";
            if (resultText.contains("Result: Warning")) {
                scanCompleteTextView.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
                status = "Warning";
            } else if (resultText.contains("Result: Malware")) {
                scanCompleteTextView.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
                status = "Malware";
            } else if (resultText.contains("Result: Clean")) {
                scanCompleteTextView.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
                status = "Clean";
            }

            String link = "http://10.10.10.45/reports_html/" + fileHash + ".html";
            databaseHelper.insertScanResult(fileName, fileHash, status, link);

            scanCompleteTextView.setVisibility(View.VISIBLE);
            scanCompleteTextView.setText(status);
            scanCompleteTextView.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
            viewDetailsButton.setVisibility(View.VISIBLE);
            viewDetailsButton.setOnClickListener(v -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                startActivity(browserIntent);
            });
        });
    }

    private void displayExistingResult(String fileName, String status, String link) {
        handler.post(() -> {
            if (status.equals("Warning")) {
                scanCompleteTextView.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
            } else if (status.equals("Malware")) {
                scanCompleteTextView.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
            } else if (status.equals("Clean")) {
                scanCompleteTextView.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
            }

            scanCompleteTextView.setVisibility(View.VISIBLE);
            scanCompleteTextView.setText(status);
            scanCompleteTextView.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
            viewDetailsButton.setVisibility(View.VISIBLE);
            viewDetailsButton.setOnClickListener(v -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                startActivity(browserIntent);
            });
        });
    }

    private void showToast(String message) {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
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












