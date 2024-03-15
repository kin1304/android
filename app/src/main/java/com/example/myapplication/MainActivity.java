package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.myapplication.Helper.ImageActivity;
import com.example.myapplication.image.FaceDetectionActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void pick_image(View view) {
        Intent intent = new Intent(this, FaceDetectionActivity.class);
        startActivity(intent);
    }
}