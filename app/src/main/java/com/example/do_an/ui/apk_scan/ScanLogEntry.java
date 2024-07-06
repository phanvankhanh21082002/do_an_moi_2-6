package com.example.do_an.ui.apk_scan;

public class ScanLogEntry {
    private String time;
    private String fileName;
    private String status;
    private String link;

    public ScanLogEntry(String time, String fileName, String status, String link) {
        this.time = time;
        this.fileName = fileName;
        this.status = status;
        this.link = link;
    }

    public String getTime() {
        return time;
    }

    public String getFileName() {
        return fileName;
    }

    public String getStatus() {
        return status;
    }

    public String getLink() {
        return link;
    }
}
