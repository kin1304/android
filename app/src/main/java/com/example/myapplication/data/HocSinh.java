package com.example.myapplication.data;

import java.io.Serializable;

public class HocSinh implements Serializable {
    public String MaHS;
    public byte[] HinhAnh;
    public float[] face_vector;

    public HocSinh(String maHS, byte[] hinhAnh, float[] face_vector) {
        MaHS = maHS;
        HinhAnh = hinhAnh;
        this.face_vector = face_vector;
    }
    public HocSinh(){}

    public HocSinh(float[] face_vector) {
        this.face_vector = face_vector;
    }
}
