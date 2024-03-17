package com.example.myapplication.image;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Bundle;
import android.renderscript.ScriptGroup;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.example.myapplication.Helper.BoxWithLabel;
import com.example.myapplication.Helper.ImageActivity;
import com.example.myapplication.data.HocSinh;
import com.example.myapplication.data.TG;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class FaceDetectionActivity extends ImageActivity {
    public String t = "";
    private ImageProcessor faceNetImageProcessor;
    private final int FACENET_INPUT_IMAGE_SIZE = 112;
    private Interpreter faceNetModelInterpreter;
    private FaceDetector face_detector;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        add_controls();

    }

    private void add_controls() {
        try {
            faceNetModelInterpreter = new Interpreter(FileUtil.loadMappedFile(this, "mobile_face_net.tflite"), new Interpreter.Options());
        } catch (IOException e) {
            e.printStackTrace();
        }
        faceNetImageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(FACENET_INPUT_IMAGE_SIZE, FACENET_INPUT_IMAGE_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                .add(new NormalizeOp(0f, 255f))
                .build();
        FaceDetectorOptions high_accuracy_opts =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .enableTracking()
                        .build();
        face_detector = FaceDetection.getClient(high_accuracy_opts) ;
    }

    @Override
    protected void runClassification(Bitmap bitmap) {
        get_output_textview().setText("");
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
                                Bitmap faceBitmap = cropToBBox(output_bitmap, face.getBoundingBox(), input_image.getRotationDegrees());

                                if (faceBitmap == null) {
                                    Log.d("GraphicOverlay", "Face bitmap null");
                                    return;
                                }

                                TensorImage tensorImage = TensorImage.fromBitmap(faceBitmap);
                                ByteBuffer faceNetByteBuffer = faceNetImageProcessor.process(tensorImage).getBuffer();
                                float[][] faceOutputArray = new float[1][192];
                                faceNetModelInterpreter.run(faceNetByteBuffer, faceOutputArray);
                                if (!TG.hocsinhs.isEmpty()) {
                                    Pair<String, Float> result = findNearestFace(faceOutputArray[0]);
                                    // if distance is within confidence
                                    if (result.second < 1.0f && result.second > 0.7f ) {
                                        t += face.getTrackingId() + " " + result.first + "\n";
                                    }
                                }
                            }
                            get_output_textview().setText(t);
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
    private Bitmap cropToBBox(Bitmap image, Rect boundingBox, int rotation) {
        int shift = 0;
        if (rotation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            image = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
        }
        if (boundingBox.top >= 0 && boundingBox.bottom <= image.getWidth()
                && boundingBox.top + boundingBox.height() <= image.getHeight()
                && boundingBox.left >= 0
                && boundingBox.left + boundingBox.width() <= image.getWidth()) {
            return Bitmap.createBitmap(
                    image,
                    boundingBox.left,
                    boundingBox.top + shift,
                    boundingBox.width(),
                    boundingBox.height()
            );
        } else return null;
    }
    private Pair<String, Float> findNearestFace(float[] vector) {

        Pair<String, Float> ret = null;
        for (HocSinh h : TG.hocsinhs) {
            final String ma = h.MaHS;
            final float[] knownVector = h.face_vector;

            float distance = 0;
            for (int i = 0; i < vector.length; i++) {
                float diff = vector[i] - knownVector[i];
                distance += diff*diff;
            }
            distance = (float) Math.sqrt(distance);
            if (ret == null || distance < ret.second) {
                ret = new Pair<>(ma, distance);
            }
        }

        return ret;

    }
}
