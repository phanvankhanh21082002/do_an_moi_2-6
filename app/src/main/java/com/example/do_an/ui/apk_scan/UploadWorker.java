package com.example.do_an.ui.apk_scan;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UploadWorker extends Worker {

    private static final String TAG = "UploadWorker";

    public UploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String filePath = getInputData().getString("filePath");
        String fileHash = getInputData().getString("fileHash");
        String fileName = getInputData().getString("fileName");

        Log.d(TAG, "Starting file upload");
        Log.d(TAG, "File Path: " + filePath);
        Log.d(TAG, "File Hash: " + fileHash);
        Log.d(TAG, "File Name: " + fileName);

        File file = new File(filePath);
        if (!file.exists()) {
            Log.e(TAG, "File does not exist: " + filePath);
            return Result.failure();
        }

        OkHttpClient client = new OkHttpClient();

        RequestBody fileBody = RequestBody.create(file, MediaType.parse("application/octet-stream"));
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("uploaded_file", fileName, fileBody)
                .addFormDataPart("file_hash", fileHash)
                .build();

        Request request = new Request.Builder()
                .url("http://35.247.145.117:3000/upload_file")
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                Log.d(TAG, "File upload successful");
                return Result.success();
            } else {
                Log.e(TAG, "File upload failed with response code: " + response.code());
                return Result.retry();
            }
        } catch (IOException e) {
            Log.e(TAG, "File upload failed", e);
            return Result.retry();
        }
    }
}
