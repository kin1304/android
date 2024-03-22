package com.example.myapplication;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.data.DBHandle;
import com.example.myapplication.data.HocSinh;
import com.example.myapplication.data.TG;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Input image size for our facenet model
    private static final int FACENET_INPUT_IMAGE_SIZE = 112;
    private float[] temp ;

    private Interpreter faceNetModelInterpreter;
    private ImageProcessor faceNetImageProcessor;
    public FaceDetector face_detector;
    ArrayList<HocSinh> array_list;
    public static DBHandle database_handle;
    public String DATABASE_NAME = "doAn";

    public String DB_SUFFIX_PATH = "/databases/";

    public TextView txt ;
    public String t="";
    public int i =-1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        add_control();
        process_copy();
        copy_database();
        copy_ma();

    }

    private void copy_ma() {
        for(int j = 0; j<=i ; j++) {
            String file_name = "ma"+String.valueOf(j)+".txt";
            String[] s;
            s = ReadFile(file_name);
            float[] f = new float[192];
            try {
                for (int i = 0; i < 192; i++) {
                    f[i] = Float.parseFloat(s[i]);
                }
                TG.hocsinhs.get(j).face_vector = f;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void copy_database() {
        Cursor cursor = database_handle.get_data("select MaHS, HinhAnh from HocSinh");
        while(cursor.moveToNext()){
            i++;
            String ma = cursor.getString(0);
            byte[] hinh = cursor.getBlob(1);
            Bitmap bm = BitmapFactory.decodeByteArray(hinh, 0, hinh.length);
            run_classification(bm,i);
            TG.hocsinhs.add(new HocSinh(ma, hinh, temp));
        }
        cursor.close();

    }
    public void stop() {
        face_detector.close();
    }
    private void run_classification(Bitmap bm, int i) {
        Bitmap output_bitmap = bm.copy(Bitmap.Config.ARGB_8888,true);
        InputImage input_image = InputImage.fromBitmap(output_bitmap,0);

        face_detector.process(input_image).addOnSuccessListener(new OnSuccessListener<List<Face>>() {
            @Override
            public void onSuccess(List<Face> faces) {
                Face face = faces.get(0);
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
                set_temp(faceOutputArray[0]);
                t="";
                for (int i=0;i<192;i++){
                    t += String.valueOf(faceOutputArray[0][i])+ " ";
                }
                String file_name = "ma"+String.valueOf(i)+".txt";
                WriteToFile(t,file_name);


            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                e.printStackTrace();
            }
        });


    }
    private void set_temp(float[] vt){
        temp = vt;
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
        try {
            faceNetModelInterpreter = new Interpreter(FileUtil.loadMappedFile(this, "mobile_face_net.tflite"), new Interpreter.Options());
        } catch (IOException e) {
            e.printStackTrace();
        }
        temp = new float[192];
        txt = findViewById(R.id.txtview);
        array_list = new ArrayList<>();
        database_handle = new DBHandle(this,"doAn",null,1);
        FaceDetectorOptions faceDetectorOptions = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                // to ensure we don't count and analyse same person again
                .enableTracking()
                .build();
        face_detector = FaceDetection.getClient(faceDetectorOptions);


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
                Toast.makeText(this, "success",Toast.LENGTH_LONG).show();
            }
        }
        catch(Exception e){
            Toast.makeText(this, "fail",Toast.LENGTH_LONG).show();

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
        catch (IOException e){
            e.printStackTrace();
        }
    }

    public void WriteToFile(String t, String file_name){

        File file = new File(get_path(file_name));
        try {
            if(file.exists()) {file.delete();}
            file.createNewFile();
            Toast.makeText(this, String.valueOf(temp[0]),Toast.LENGTH_LONG).show();
            OutputStream output_file = new FileOutputStream(get_path(file_name));
            OutputStreamWriter ghi = new OutputStreamWriter(output_file);
            ghi.write(t);
            ghi.flush();
            ghi.close();

        }
        catch (IOException e){
            Toast.makeText(this,"that bai",Toast.LENGTH_LONG);
            e.printStackTrace();
        }
    }
    public String[] ReadFile(String file_name){
        try{
            File file = new File(get_path(file_name));

            InputStream input_file = new FileInputStream(file);
            InputStreamReader doc = new InputStreamReader(input_file);
            BufferedReader read = new BufferedReader(doc);
            String[] s = read.readLine().split(" ");
            doc.close();
            return s;
        }catch(IOException e){
            Toast.makeText(this,"that bai",Toast.LENGTH_LONG);
            return null;
        }
    }


}