package com.example.myapplication.Helper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;

import com.example.myapplication.R;

import java.io.IOException;

public class ImageActivity extends AppCompatActivity {
    private int REQUEST_CODE_IMAGE = 1000;
    private ImageView img;
    private ListView lv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        on_create();

        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            }
        }
    }

    private void on_create() {
        img = findViewById(R.id.hinh_sinh_vien);
        lv = findViewById(R.id.list_sinh_vien);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d(ImageActivity.class.getSimpleName(), "grant result for"+ permissions[0]);
    }

    public void pick_image(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_CODE_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            if(requestCode == REQUEST_CODE_IMAGE){
                Uri uri = data.getData();
                Bitmap bitmap = load_from_uri(uri);
                img.setImageBitmap(bitmap);
            }
        }
    }

    private Bitmap load_from_uri(Uri uri){
        Bitmap bitmap = null;

        try {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
                bitmap = ImageDecoder.decodeBitmap(source);

            }
            else{
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            }
        }catch(IOException e){
            e.printStackTrace();
        }

        return bitmap;
    }
}