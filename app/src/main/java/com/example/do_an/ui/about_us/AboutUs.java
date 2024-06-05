package com.example.do_an.ui.about_us;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.do_an.R;

import org.w3c.dom.Text;


public class AboutUs extends Fragment {

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about_us, container, false);

        TextView textView = view.findViewById(R.id.about_us_text1);
        String text = "This app was developed by five students of the last year of FPT University. " +
                "\n\nThe institution’s staunch commitment to creating a nurturing learning environment and stimulating intellectual curiosity.";
        textView.setText(text);

        textView = view.findViewById(R.id.about_us_text2);
        text = "This application was built to detect malicious APK files and should be useful for searching for possible malware on Android devices.";
        textView.setText(text);


        return view;
    }
}
