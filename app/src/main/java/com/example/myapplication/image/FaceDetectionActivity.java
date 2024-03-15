package com.example.myapplication.image;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.renderscript.ScriptGroup;

import androidx.annotation.NonNull;

import com.example.myapplication.Helper.BoxWithLabel;
import com.example.myapplication.Helper.ImageActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.ArrayList;
import java.util.List;

public class FaceDetectionActivity extends ImageActivity {

    private FaceDetector face_detector;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FaceDetectorOptions high_accuracy_opts =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .enableTracking()
                        .build();
        face_detector = FaceDetection.getClient(high_accuracy_opts) ;
    }

    @Override
    protected void runClassification(Bitmap bitmap) {
        Bitmap output_bitmap = bitmap.copy(Bitmap.Config.ARGB_8888,true);
        InputImage input_image = InputImage.fromBitmap(output_bitmap,0);
        face_detector.process(input_image)
                .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                    @Override
                    public void onSuccess(List<Face> faces) {
                        if(faces.isEmpty()){
                            get_output_textview().setText("No faces detected");
                        } else{
                            List<BoxWithLabel> boxes = new ArrayList<>();
                            for( Face face : faces){
                                BoxWithLabel box_with_label = new BoxWithLabel(
                                        face.getBoundingBox(),
                                        face.getTrackingId() + ""
                                );
                                boxes.add(box_with_label);
                            }
                            draw_detection_result(boxes, bitmap);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        e.printStackTrace();
                    }
                });
    }
}
