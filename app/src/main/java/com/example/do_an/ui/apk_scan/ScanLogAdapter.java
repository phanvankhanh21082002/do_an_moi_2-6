package com.example.do_an.ui.apk_scan;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an.R;

import java.util.List;

public class ScanLogAdapter extends RecyclerView.Adapter<ScanLogAdapter.ViewHolder> {
    private List<ScanLogEntry> scanLogEntries;
    private Context context;

    public ScanLogAdapter(List<ScanLogEntry> scanLogEntries, Context context) {
        this.scanLogEntries = scanLogEntries;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_scan_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScanLogEntry entry = scanLogEntries.get(position);
        holder.tvTime.setText(entry.getTime());
        holder.tvFileName.setText(entry.getFileName());
        holder.tvStatus.setText(entry.getStatus());
        holder.tvLink.setText("Chi tiết file");

        // Set the background color based on status
        switch (entry.getStatus()) {
            case "Warning":
                holder.itemView.setBackgroundColor(context.getResources().getColor(android.R.color.holo_orange_light));
                break;
            case "Malware":
                holder.itemView.setBackgroundColor(context.getResources().getColor(android.R.color.holo_red_light));
                break;
            case "Clean":
                holder.itemView.setBackgroundColor(context.getResources().getColor(android.R.color.holo_green_light));
                break;
        }

        // Set tooltip using a PopupWindow
        holder.tvFileName.setOnLongClickListener(v -> {
            showPopupWindow(v, entry.getFileName());
            return true;
        });

        // Open the link in a browser
        holder.tvLink.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(entry.getLink()));
            context.startActivity(browserIntent);
        });
    }

    private void showPopupWindow(View anchorView, String text) {
        // Inflate the custom layout/view
        View customView = LayoutInflater.from(context).inflate(R.layout.tooltip_layout, null);

        // Initialize a new instance of popup window
        PopupWindow popupWindow = new PopupWindow(customView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        // Set an elevation value for popup window
        popupWindow.setElevation(5.0f);

        // Get a reference for the custom view close button
        TextView tooltipTextView = customView.findViewById(R.id.tooltipTextView);
        tooltipTextView.setText(text);

        // Show the popup window at the location of the anchor view
        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, location[0], location[1] - anchorView.getHeight());

        // Dismiss the popup window when touched
        customView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                popupWindow.dismiss();
                return true;
            }
            return false;
        });
    }



    @Override
    public int getItemCount() {
        return scanLogEntries.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvFileName, tvStatus, tvLink;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvLink = itemView.findViewById(R.id.tvLink);
        }
    }
}
