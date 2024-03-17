package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.myapplication.Helper.BoxWithLabel;
import com.example.myapplication.Helper.DatabaseHandle;
import com.example.myapplication.Helper.ImageActivity;
import com.example.myapplication.data.DBHandle;
import com.example.myapplication.data.HocSinh;
import com.example.myapplication.image.FaceDetectionActivity;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public interface FaceRecognitionCallback {
        void onFaceRecognised(Face face, float probability, String name);
        void onFaceDetected(Face face, Bitmap faceBitmap, float[] vector);
    }
    // Input image size for our facenet model
    private static final int FACENET_INPUT_IMAGE_SIZE = 112;
    List<HocSinh> recognisedFaceList = new ArrayList();
    private Interpreter faceNetModelInterpreter;
    private ImageProcessor faceNetImageProcessor;

    private FaceRecognitionCallback callback;
    public FaceDetector face_detector;
    ArrayList<HocSinh> array_list;
    public static DBHandle database_handle;
    public String DATABASE_NAME = "doAn";
    public String output_name = "danh_sach";
    public String DB_SUFFIX_PATH = "/databases/";
    public static SQLiteDatabase database = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        add_control();
        process_copy();
        copy_database();
    }

    private void copy_database() {
        Cursor cursor = database_handle.get_data("select MaHS, HinhAnh from HocSinh");
        while(cursor.moveToNext()){
            String ma = cursor.getString(0);
            byte[] hinh = cursor.getBlob(1);
            Bitmap bm = BitmapFactory.decodeByteArray(hinh, 0, hinh.length);
            float[] vt = run_classification(bm);
            array_list.add(new HocSinh(ma, hinh, vt));
        }
    }

    private float[] run_classification(Bitmap bm) {
        Bitmap output_bitmap = bm.copy(Bitmap.Config.ARGB_8888,true);
        InputImage input_image = InputImage.fromBitmap(output_bitmap,0);
        final float[][] temp = new float[1][192];
        face_detector.process(input_image).addOnSuccessListener(new OnSuccessListener<List<Face>>() {
            @Override
            public void onSuccess(List<Face> faces) {
                if(faces.isEmpty()){

                } else{
                    for( Face face : faces){
                        int rotationDegrees = input_image.getRotationDegrees();

                        Bitmap faceBitmap = cropToBBox(output_bitmap, face.getBoundingBox(), rotationDegrees);

                        if (faceBitmap == null) {
                            Log.d("GraphicOverlay", "Face bitmap null");
                            return;
                        }

                        TensorImage tensorImage = TensorImage.fromBitmap(faceBitmap);
                        ByteBuffer faceNetByteBuffer = faceNetImageProcessor.process(tensorImage).getBuffer();
                        float[][] faceOutputArray = new float[1][192];
                        faceNetModelInterpreter.run(faceNetByteBuffer, faceOutputArray);
                        temp[0] = faceOutputArray[0];

                    }

                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                temp[0] = null;
                e.printStackTrace();
            }
        });
        return temp[0];
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

    private void add_control() {

        array_list = new ArrayList<>();
        database_handle = new DBHandle(this,"doAn",null,1);
        FaceDetectorOptions high_accuracy_opts =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .enableTracking()
                        .build();
        face_detector = FaceDetection.getClient(high_accuracy_opts);
        try {
            faceNetModelInterpreter = new Interpreter(FileUtil.loadMappedFile(this, "mobile_face_net.tflite"), new Interpreter.Options());
        } catch (IOException e) {
            e.printStackTrace();
        }
        faceNetImageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(FACENET_INPUT_IMAGE_SIZE, FACENET_INPUT_IMAGE_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                .add(new NormalizeOp(0f, 255f))
                .build();

    }

    public void pick_image(View view) {
        Intent intent = new Intent(this, FaceDetectionActivity.class);
        startActivity(intent);
    }
    public String get_path(String DATABASE_NAME){
        String s = getApplicationInfo().dataDir;
        return s + DB_SUFFIX_PATH + DATABASE_NAME ;
    }
    public void process_copy(){
        try{
            File file = new File(get_path(DATABASE_NAME));
            if (!file.exists()){
                copy_database_from_assest();
                Toast.makeText(this, "success",Toast.LENGTH_LONG);
            }
        }
        catch(Exception e){
            Toast.makeText(this, "fail",Toast.LENGTH_LONG);

        }
    }
    public void copy_database_from_assest(){
        try{
            InputStream input_file = getAssets().open(DATABASE_NAME);
            File file = new File(get_path(""));
            if(!file.exists()){
                file.mkdir();
            }
            OutputStream output_file = new FileOutputStream(get_path(DATABASE_NAME));
            byte[] buffer = new byte[1024];
            int length;
            while((length = input_file.read(buffer))>0){
                output_file.write(buffer,0,length);
            }
            output_file.flush();
            output_file.close();
            input_file.close();

        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

}