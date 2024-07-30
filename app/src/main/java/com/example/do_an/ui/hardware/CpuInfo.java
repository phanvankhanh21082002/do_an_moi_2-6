package com.example.do_an.ui.hardware;


import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.do_an.R;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


public class CpuInfo extends AppCompatActivity {

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cpu_info);
        TextView cpuInfoTextview = findViewById(R.id.cpu_info_text);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        cpuInfoTextview.setText(getCpuInfo());

    }
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String getCpuInfo() {
        StringBuilder cpuInfo = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo"));
            String line;
            while ((line = br.readLine()) != null) {
                cpuInfo.append(line).append("\n");
            }
            br.close();
        } catch (IOException e) {
            Log.e("CpuInfo", "Error reading CPU info", e);
        }
        return cpuInfo.toString();

    }


}
