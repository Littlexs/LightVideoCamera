package com.example.bdxk.lightvideorecord;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.example.bdxk.lightvideorecord.ui.MainCameraActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void goRecoding(View view) {
        Intent intent = new Intent(MainActivity.this, MainCameraActivity.class);
        startActivity(intent);
    }
}
