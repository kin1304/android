package com.example.myapplication.Helper;

import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class DatabaseHandle extends AppCompatActivity {
    public String DATABASE_NAME = "doAn";
    public String output_name = "danh_sach";
    public String DB_SUFFIX_PATH = "/databases/";
    public static SQLiteDatabase database = null;

    public String get_path(String DATABASE_NAME){
        String s = getApplicationInfo().dataDir;
        return s + DB_SUFFIX_PATH + DATABASE_NAME ;
    }
    public DatabaseHandle(){

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
            OutputStream output_file = new FileOutputStream(get_path(""));
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
