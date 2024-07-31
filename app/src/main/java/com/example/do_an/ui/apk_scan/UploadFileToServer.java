package com.example.do_an.ui.apk_scan;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.RequestBody;

public class UploadFileToServer {
    private static final String TAG = "UploadFileToServer";
    public static RequestBody createRequestBodyFromUri(Context context, Uri fileUri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
        String mimeType = context.getContentResolver().getType(fileUri);
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        MediaType mediaType = MediaType.parse(mimeType);
        byte[] bytes = readBytes(inputStream);
        return RequestBody.create(mediaType, bytes);
    }

    private static byte[] readBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    public static String saveUriToFile(Context context, Uri uri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            throw new IOException("Failed to open input stream from URI");
        }

        File file = new File(context.getCacheDir(), getFileName(context, uri));
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } finally {
            inputStream.close();
        }
        Log.d(TAG, "File saved locally: " + file.getAbsolutePath());
        return file.getAbsolutePath();
    }

    @SuppressLint("Range")
    private static String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } catch (Exception e) {
                e.printStackTrace();
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

