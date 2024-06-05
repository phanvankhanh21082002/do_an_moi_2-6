package com.example.do_an.ui.apk_scan;


import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;



import androidx.fragment.app.Fragment;
import com.example.do_an.R;

public class APKShow extends Fragment {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_apk_show, container, false);
        Intent intent = new Intent(getActivity(), ScanAPK.class);
        startActivity(intent);
        return view;
    }

}
