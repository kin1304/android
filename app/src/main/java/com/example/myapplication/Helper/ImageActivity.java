package com.example.myapplication.Helper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.myapplication.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.io.IOException;
import java.util.List;

public class ImageActivity extends AppCompatActivity {
    private int REQUEST_CODE_IMAGE = 1000;
    private ImageView input_image_view;
    private TextView output_textview;

    private ImageLabeler imagelabeler;

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
        input_image_view = findViewById(R.id.hinh_sinh_vien);
        output_textview = findViewById(R.id.output_textview);
        imagelabeler = ImageLabeling.getClient(new ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.7f)
                .build()
        );

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
                input_image_view.setImageBitmap(bitmap);
                runClassification(bitmap);
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
    protected ImageView get_input_image_view(){return input_image_view;}
    protected TextView get_output_textview(){return output_textview;}
    protected void runClassification(Bitmap bitmap){

    }
    protected void draw_detection_result(List<BoxWithLabel> boxes, Bitmap bitmap){
        Bitmap output_bitmap = bitmap.copy(Bitmap.Config.ARGB_8888,true);
        Canvas canvas = new Canvas(output_bitmap);
        Paint pen_rect = new Paint();
        pen_rect.setColor(Color.RED);
        pen_rect.setStyle(Paint.Style.STROKE);
        pen_rect.setStrokeWidth(8f);

        Paint pen_label = new Paint();
        pen_label.setColor(Color.YELLOW);
        pen_label.setStyle(Paint.Style.FILL_AND_STROKE);
        pen_label.setTextSize(96f);

        for (BoxWithLabel box_with_label : boxes){
            canvas.drawRect(box_with_label.rect, pen_rect);

            //Rect
            Rect label_size = new Rect(0,0,0,0);
            pen_label.getTextBounds(box_with_label.label,0,box_with_label.label.length(),label_size);
            float font_size = pen_label.getTextSize() * box_with_label.rect.width() / label_size.width();
            if(font_size < pen_label.getTextSize()){
                pen_label.setTextSize(font_size);
            }

            float margin = (box_with_label.rect.width() - label_size.width()) / 2.0F;
            if (margin < 0F) margin = 0F;
            canvas.drawText(
                    box_with_label.label, box_with_label.rect.left + margin,
                    box_with_label.rect.top + label_size.height(), pen_label
            );

        }
        get_input_image_view().setImageBitmap(output_bitmap);

    }
}