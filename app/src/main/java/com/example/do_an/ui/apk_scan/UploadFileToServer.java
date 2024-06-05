package com.example.do_an.ui.apk_scan;
import android.content.Context;
import android.net.Uri;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import okhttp3.MediaType;
import okhttp3.RequestBody;

public class UploadFileToServer {
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
}

