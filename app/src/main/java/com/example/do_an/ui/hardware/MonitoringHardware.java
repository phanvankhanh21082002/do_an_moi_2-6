package com.example.do_an.ui.hardware;

import android.annotation.SuppressLint;
import android.app.ActivityManager;


import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;


import android.provider.Settings;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;


import androidx.fragment.app.Fragment;
import com.example.do_an.R;

import java.io.BufferedReader;
import java.io.IOException;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MonitoringHardware extends Fragment{

    private ProgressBar storageProgressBar;
    private ProgressBar ramProgressBar;
    private TextView storageTextView;
    private TextView ramTextView;
    private TextView cpuPercentageTextView;
    private ProgressBar cpuProgressBar;
    private Handler handler;
    private Runnable updateRunnable;
    private static final String TAG = "MonitoringHardware";
    private TextView cpuCoreTextView;
    private TextView cpuDetailsTextView;
    private int coreCount = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_hardware_monitoring, container, false);

        storageProgressBar = view.findViewById(R.id.storage_progress);
        ramProgressBar = view.findViewById(R.id.ram_progress);
        storageTextView = view.findViewById(R.id.storage_text);
        ramTextView = view.findViewById(R.id.ram_text);
        cpuDetailsTextView = view.findViewById(R.id.cpu_details);

        cpuCoreTextView = view.findViewById(R.id.cpu_core);
        cpuPercentageTextView = view.findViewById(R.id.cpu_percentage);
        cpuProgressBar = view.findViewById(R.id.cpu_progress_bar);

        handler = new Handler(Looper.getMainLooper());

        if (!hasUsageStatsPermission()) {
            requestRequiredPermissions();
        } else {
            startCpuUsageMonitoring();
        }

        updateStorageInfo();
        updateRAMInfo();
        fetchCpuCoreCount();

        return view;
    }

    public boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) requireContext().getSystemService(Context.APP_OPS_SERVICE);
        int usageStatsMode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), requireContext().getPackageName());
        return usageStatsMode == AppOpsManager.MODE_ALLOWED;
    }

    private void requestRequiredPermissions() {
        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
    }

    @Override
    public void onResume() {
        super.onResume();
        startCpuUsageMonitoring();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopCpuUsageMonitoring();
    }

    private void startCpuUsageMonitoring() {
        if (updateRunnable == null) {
            updateRunnable = new Runnable() {
                @Override
                public void run() {
                    updateCpuDetails();
                    updateCpuTotalUsage();
                    updateCpuProcess();
                    handler.postDelayed(this, 1000); // Update every 1 second
                }
            };
        }
        handler.post(updateRunnable);
    }

    private void stopCpuUsageMonitoring() {
        if (handler != null && updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
    }


    @SuppressLint("DefaultLocale")
    private void updateStorageInfo() {
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long totalBytes = stat.getBlockSizeLong() * stat.getBlockCountLong();
        long availableBytes = stat.getBlockSizeLong() * stat.getAvailableBlocksLong();
        long usedBytes = totalBytes - availableBytes;

        float totalGB = totalBytes / (1024f * 1024f * 1024f);
        float usedGB = usedBytes / (1024f * 1024f * 1024f);
        int storagePercentage = (int) ((usedBytes / (float) totalBytes) * 100);

        storageProgressBar.setProgress(storagePercentage);
        storageTextView.setText(String.format("%.2f GB used / %.2f GB (%d%%)", usedGB, totalGB, storagePercentage));
    }

    private void updateRAMInfo() {
        getActivity();
        ActivityManager activityManager = (ActivityManager) requireActivity().getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);

        long totalRAM = memoryInfo.totalMem;
        long availableRAM = memoryInfo.availMem;
        long usedRAM = totalRAM - availableRAM;

        float totalGB = totalRAM / (1024f * 1024f * 1024f);
        float usedGB = usedRAM / (1024f * 1024f * 1024f);
        int ramPercentage = (int) ((usedRAM / (float) totalRAM) * 100);

        ramProgressBar.setProgress(ramPercentage);
        ramTextView.setText(String.format("%.2f GB used / %.2f GB (%d%%)", usedGB, totalGB, ramPercentage));
    }

    private void fetchCpuCoreCount() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            int cores = Runtime.getRuntime().availableProcessors();
            coreCount = cores; // store the number of cores for calculation
            handler.post(() -> {
                if (cores > 0) {
                    cpuCoreTextView.setText(cores + " cores");
                } else {
                    cpuCoreTextView.setText("Error fetching core count");
                }
            });
        });
    }

    private String parseCpuDetails(String result) {
        String[] lines = result.split("\n");
        for (String line : lines) {
            if (line.matches("\\d+%cpu.*")) {
                return "Explanation: each cpu core has 100% total\nTotal CPU usage:%cpu \nUser processes:%user \nSystem processes:%sys\nIdle CPUs:%idle\n\n" + line;
            }
        }
        return "No CPU details found";
    }

    private void updateCpuDetails() {
        new Thread(() -> {
            String result = getCpuDetails();
            if (result != null) {
                Log.d("MonitoringHardware", "CPU Details: " + result);
                String cpuDetails = parseCpuDetails(result);
                requireActivity().runOnUiThread(() -> {
                    cpuDetailsTextView.setText(cpuDetails);
                });
            } else {
                Log.e("MonitoringHardware", "Failed to get CPU details");
                requireActivity().runOnUiThread(() -> {
                    cpuDetailsTextView.setText("Failed to get CPU details");
                });
            }
        }).start();
    }

    private String getCpuDetails() {
        StringBuilder output = new StringBuilder();
        Process process = null;
        try {
            String[] command = {"/system/bin/sh", "-c", "top -b -n 1"};
            process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            reader.close();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            Log.e("MonitoringHardware", "Error running command", e);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return output.toString();
    }

    private void updateCpuTotalUsage() {
        new Thread(() -> {
            String result = getCpuTotalUsage();
            if (result != null && !result.isEmpty()) {
                double totalCpuUsage = calculateTotalCpuUsage(result);
                if (isAdded()) { // Check if the fragment is added to its activity
                    requireActivity().runOnUiThread(() -> {
                        cpuPercentageTextView.setText(String.format("%.2f%%", totalCpuUsage));
                        cpuProgressBar.setProgress((int) totalCpuUsage);
                    });
                }
            } else {
                if (isAdded()) { // Check if the fragment is added to its activity
                    requireActivity().runOnUiThread(() -> cpuPercentageTextView.setText("No CPU details found"));
                }
            }
        }).start();
    }
    private double calculateTotalCpuUsage(String idleDetails) {
        if (coreCount == 0) {
            return 0; // Avoid division by zero if core count is not set
        }
        double idleTime = Double.parseDouble(idleDetails.replace("%idle", "").trim());
        return (coreCount * 100) - idleTime;
    }

    private String getCpuTotalUsage() {
        StringBuilder output = new StringBuilder();
        Process process = null;
        try {
            String[] command = {"/system/bin/sh", "-c", "top -b -n 1"};
            process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            List<String> lines = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                Log.d("MonitoringHardware", line);
                lines.add(line.trim());
            }
            reader.close();
            process.waitFor();

            if (lines.size() > 3) {
                String[] cpuLineParts = lines.get(3).split("\\s+");
                for (String part : cpuLineParts) {
                    if (part.contains("%idle")) {
                        output.append(part);
                        break;
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            Log.e("MonitoringHardware", "Error executing command", e);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        Log.d("MonitoringHardware", "Final output: " + output.toString());
        return output.toString();
    }




    private void updateCpuProcess() {
        Executors.newSingleThreadExecutor().execute(() -> {
            String result = runTopCommand();
            if (result != null) {
                Log.d(TAG, "Command output: " + result);
                if (isAdded() && getActivity() != null) {
                    requireActivity().runOnUiThread(() -> updateTableLayout(result));
                }
            } else {
                Log.e(TAG, "Failed to get CPU process");
            }
        });
    }

    private void updateTableLayout(String output) {
        TableLayout tableLayout = requireView().findViewById(R.id.cpu_info_table);

        // Clear existing rows except the header
        int childCount = tableLayout.getChildCount();
        if (childCount > 1) {
            tableLayout.removeViews(1, childCount - 1);
        }

        String[] lines = output.split("\n");
        for (int i = 1; i < lines.length; i++) { // Skip header line
            String line = lines[i].trim();
            String[] parts = line.split("\\s+");
            if (parts.length >= 5) {
                TableRow row = new TableRow(getContext());

                TextView pid = new TextView(getContext());
                pid.setText(parts[0]);
                pid.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
                row.addView(pid);

                TextView cpu = new TextView(getContext());
                cpu.setText(parts[1]);
                cpu.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
                row.addView(cpu);

                TextView mem = new TextView(getContext());
                mem.setText(parts[2]);
                mem.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
                row.addView(mem);

                TextView time = new TextView(getContext());
                time.setText(parts[3]);
                time.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
                row.addView(time);

                TextView comm = new TextView(getContext());
                comm.setText(parts[4]);
                comm.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
                row.addView(comm);

                tableLayout.addView(row);
            }
        }
    }

    private String runTopCommand() {
        StringBuilder output = new StringBuilder();
        Process process = null;
        try {
            String[] command = {"/system/bin/sh", "-c", "top -n 1 -b"};
            Log.d(TAG, "Executing command: " + command[2]);
            process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            boolean headerPassed = false;
            while ((line = reader.readLine()) != null) {
                Log.d(TAG, "Command output line: " + line);
                // Filter and format relevant lines
                if (line.contains("PID") && line.contains("USER") && line.contains("%CPU")) {
                    headerPassed = true;
                    output.append(String.format("%-5s %-5s %-5s %-7s %-10s\n", "PID", "%CPU", "%MEM", "TIME+", "COMM"));
                } else if (headerPassed && !line.trim().isEmpty()) {
                    String[] parts = line.trim().split("\\s+", 12); // Adjusted to 12 columns to handle spaces in COMMAND
                    if (parts.length >= 12) {
                        output.append(String.format("%-5s %-5s %-5s %-7s %-10s\n", parts[0], parts[8], parts[9], parts[10], parts[11]));
                    }
                }
            }
            reader.close();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "Error running command", e);
            return null;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return output.toString();
    }

}
