package com.example.do_an;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.net.HttpURLConnection;
//import java.net.URL;
//
//import android.annotation.SuppressLint;
//import android.app.Activity;
//import android.app.ProgressDialog;
//import android.content.ContentUris;
//import android.content.Context;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.database.Cursor;
//import android.net.Uri;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.Environment;
//import android.provider.DocumentsContract;
//import android.provider.MediaStore;
//import android.provider.Settings;
//import android.util.Log;
//import android.view.View;
//import android.view.View.OnClickListener;
//import android.widget.Button;
//import android.widget.TextView;
//import android.widget.Toast;
//import androidx.core.app.ActivityCompat;
//import android.Manifest;
//
//import com.github.angads25.filepicker.model.DialogConfigs;
//import com.github.angads25.filepicker.model.DialogProperties;
//import com.github.angads25.filepicker.view.FilePickerDialog;
//
//import retrofit2.Call;
//import retrofit2.Callback;
//import retrofit2.Response;
//import retrofit2.Retrofit;
//
//public class MainActivity extends Activity {
//
//    final int ACTIVITY_CHOOSE_FILE = 1;
//    static int uploadResponseCode = 0;
//    String selectedFilePath = null;
//
//    Button fileSelectorButton;
//    TextView selectedFileTextView;
//    Button uploadButton;
//    ProgressDialog uploadScanDialog;
//    ProgressDialog downloadScanDialog;
//    TextView scanCompleteTextView;
//    Button downloadButton;
//    TextView downloadedTextView;
//    FilePickerDialog dialog;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        ActivityCompat.requestPermissions(this,
//                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                        Manifest.permission.READ_EXTERNAL_STORAGE},
//                PackageManager.PERMISSION_GRANTED);
//
//        fileSelectorButton = (Button) findViewById(R.id.fileSelectorButton);
//        selectedFileTextView = (TextView) findViewById(R.id.selectedFileTextView);
//        uploadButton = (Button) findViewById(R.id.uploadButton);
//        scanCompleteTextView = (TextView) findViewById(R.id.scanCompleteTextView);
//        downloadButton = (Button) findViewById(R.id.downloadButton);
//        downloadedTextView = (TextView) findViewById(R.id.downloadedSizeTextView);
//
//        fileSelectorButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    DialogProperties properties = new DialogProperties();
//                    properties.selection_mode = DialogConfigs.SINGLE_MODE;
//                    properties.selection_type = DialogConfigs.FILE_SELECT;
//                    properties.root = Environment.getExternalStorageDirectory();
//                    properties.error_dir = properties.root;
//                    properties.offset = properties.root;
//                    properties.extensions = new String[]{"apk"};
//
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
//                        try {
//                            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
//                            intent.addCategory("android.intent.category.DEFAULT");
//                            intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
//                            startActivityForResult(intent, 2296);
//                        } catch (Exception e) {
//                            Intent intent = new Intent();
//                            intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
//                            startActivityForResult(intent, 2296);
//                        }
//                    }
//
//                    dialog = new FilePickerDialog(MainActivity.this, properties);
//                    dialog.setDialogSelectionListener(files -> {
//                        //files is the array of the paths of files selected by the Application User.
//                        if (files != null) {
//                            File selectedFile = new File(files[0]);
//                            selectedFilePath= selectedFile.toString();
//                            selectedFileTextView.setVisibility(View.VISIBLE);
//                            selectedFileTextView.setText(selectedFilePath);
//                        } else {
//                            System.out.printf("wrong choose file");
//                        }
//                    });
//                    dialog.show();
//            }
//        });
//
//        uploadButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (selectedFilePath != null) {
//                    uploadScanDialog = ProgressDialog.show(MainActivity.this, "",
//                            "Upload and Scan Process Started...", true);
//                    new Thread(new Runnable() {
//                        public void run() {
//                            //new thread to start the activity
//                            uploadResponseCode = UploadFileToServer
//                                    .uploadFile((String) selectedFilePath);
//                            uploadScanDialog.dismiss();
//                            if (uploadResponseCode == 200) {
//                                runOnUiThread(new Runnable() {
//                                    public void run() {
//                                        scanCompleteTextView
//                                                .setText("File Scan Complete!!");
//                                        uploadResponseCode = 0;
//                                        downloadButton.setVisibility(View.VISIBLE);
//                                    }
//                                });
//                            } else {
//                                runOnUiThread(new Runnable() {
//                                    public void run() {
//                                        scanCompleteTextView
//                                                .setText("Oops!! Error uploading file.");
//                                        uploadResponseCode = 0;
//                                    }
//                                });
//                            }
//                        }
//                    }).start();
//                } else {
//                    Toast.makeText(MainActivity.this,
//                                    "Please select a file to upload!!", Toast.LENGTH_LONG)
//                            .show();
//                }
//            }
//        });
//
//        downloadButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                File file = new File(selectedFilePath);
//                String parentPath = file.getParent();
//                downloadScanDialog = ProgressDialog.show(MainActivity.this, "",
//                        "Downloading Scan Results...", true);
//                String fileName = new File(selectedFilePath).getName();
//                String URL_STRING = "http://34.126.66.46/reports/"+fileName+".txt";
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//                            URL url = new URL(URL_STRING);
//                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//                            connection.setRequestMethod("GET");
//
//                            int responseCode = connection.getResponseCode();
//                            if (responseCode == HttpURLConnection.HTTP_OK) {
//                                InputStream inputStream = connection.getInputStream();
//                                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
//                                StringBuilder stringBuilder = new StringBuilder();
//                                String line;
//                                while ((line = reader.readLine()) != null) {
//                                    stringBuilder.append(line+"\n");
//                                }
//                                reader.close();
//                                String responseBody = stringBuilder.toString();
//                                System.out.println(responseBody);
//                                runOnUiThread(new Runnable() {
//                                    @Override
//                                    public void run() {
//
//                                    }
//                                });
//                            }
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }).start();
//            }
//        });
//    }
//
//    public static String getPathFromUri(Context context, Uri uri) {
//        // Kiểm tra xem URI có thể được sử dụng với DocumentProvider không
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri)) {
//            // Nếu là URI của ExternalStorageProvider, ta cần lấy ID của tài liệu
//            if (isExternalStorageDocument(uri)) {
//                final String docId = DocumentsContract.getDocumentId(uri);
//                final String[] split = docId.split(":");
//                final String type = split[0];
//
//                if ("primary".equalsIgnoreCase(type)) {
//                    return context.getExternalFilesDir(null) + "/" + split[1];
//                }
//            }
//            // Nếu là URI của DownloadsProvider, ta cần lấy ID của tài liệu
//            else if (isDownloadsDocument(uri)) {
//                final String id = DocumentsContract.getDocumentId(uri);
//                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));
//                return getDataColumn(context, contentUri, null, null);
//            }
//            // Nếu là URI của MediaProvider, ta cần lấy ID của tài liệu
//            else if (isMediaDocument(uri)) {
//                final String docId = DocumentsContract.getDocumentId(uri);
//                final String[] split = docId.split(":");
//                final String type = split[0];
//
//                Uri contentUri = null;
//                if ("image".equals(type)) {
//                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
//                } else if ("video".equals(type)) {
//                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
//                } else if ("audio".equals(type)) {
//                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
//                }
//
//                final String selection = "_id=?";
//                final String[] selectionArgs = new String[]{split[1]};
//
//                return getDataColumn(context, contentUri, selection, selectionArgs);
//            }
//        }
//        // Nếu không phải là DocumentProvider, ta sử dụng cách tiêu chuẩn để lấy đường dẫn
//        else if ("content".equalsIgnoreCase(uri.getScheme())) {
//            return getDataColumn(context, uri, null, null);
//        }
//        // Nếu là URI của FileProvider, ta lấy đường dẫn trực tiếp từ URI
//        else if ("file".equalsIgnoreCase(uri.getScheme())) {
//            return uri.getPath();
//        }
//        return null;
//    }
//
//    private static boolean isExternalStorageDocument(Uri uri) {
//        return "com.android.externalstorage.documents".equals(uri.getAuthority());
//    }
//
//    private static boolean isDownloadsDocument(Uri uri) {
//        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
//    }
//
//    private static boolean isMediaDocument(Uri uri) {
//        return "com.android.providers.media.documents".equals(uri.getAuthority());
//    }
//    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
//        Cursor cursor = null;
//        String column = "_data";
//        String[] projection = {column};
//
//        try {
//            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
//            if (cursor != null && cursor.moveToFirst()) {
//                int columnIndex = cursor.getColumnIndexOrThrow(column);
//                return cursor.getString(columnIndex);
//            }
//        } finally {
//            if (cursor != null) {
//                cursor.close();
//            }
//        }
//        return null;
//    }
//    // reset all the controls to null
//    public void reset() {
//        runOnUiThread(new Runnable() {
//            public void run() {
//                downloadButton.setVisibility(View.GONE);
//                selectedFilePath = null;
//                selectedFileTextView.setText(null);
//                scanCompleteTextView.setText(null);
//            }
//        });
//    }
//}
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.widget.TextView;
import android.app.ProgressDialog;
import android.widget.Toast;

import com.github.angads25.filepicker.view.FilePickerDialog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.*;

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
    Button downloadButton;
    TextView downloadedTextView;
    Button showDatabaseButton;
    DatabaseHelper databaseHelper;
    FilePickerDialog dialog;
    String fileName=null;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE},
                PackageManager.PERMISSION_GRANTED);

        fileSelectorButton = (Button) findViewById(R.id.fileSelectorButton);
        selectedFileTextView = (TextView) findViewById(R.id.selectedFileTextView);
        uploadButton = (Button) findViewById(R.id.uploadButton);
        scanCompleteTextView = (TextView) findViewById(R.id.scanCompleteTextView);
        showDatabaseButton = findViewById(R.id.showDatabaseButton);
        databaseHelper = new DatabaseHelper(this);

        setupFilePickerLauncher();
        fileSelectorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickFile();
            }
        });

        showDatabaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ShowDatabaseActivity.class);
                startActivity(intent);
            }
        });
    }

    private void setupFilePickerLauncher() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri fileUri = result.getData().getData();
                        fileName = getFileName(fileUri);
                        selectedFileTextView.setVisibility(View.VISIBLE);
                        selectedFileTextView.setText(fileName);
                        uploadButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
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
        ProgressDialog dialog = new ProgressDialog(MainActivity.this);
        dialog.setMessage("Uploading file...");
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
                        .url("http://34.126.66.46:3000/upload_file")
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
                            runOnUiThread(() -> {
                                checkForReport(fileName, dialog);
                            });
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
    private void checkForReport(String fileName, ProgressDialog dialog) {
        executor.execute(() -> {
            boolean reportReady = false;
            while (!reportReady) {
                try {
                    URL url = new URL("http://34.126.66.46/reports/" + fileName + ".txt");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        reportReady = true; // Set flag to true to break the loop
                        handler.post(() -> {
                            dialog.dismiss();
                            showToast("Report is ready!");
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
}



