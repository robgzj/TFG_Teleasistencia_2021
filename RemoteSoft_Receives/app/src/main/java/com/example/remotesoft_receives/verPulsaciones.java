package com.example.remotesoft_receives;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class verPulsaciones extends AppCompatActivity {

    Button btn_atras;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ver_pulsaciones);

        btn_atras= findViewById(R.id.boton_atras);
        btn_atras.setOnClickListener(v -> openMainActivity());
    }

    public void openMainActivity(){
        Intent intent =new Intent();
        intent.setClass(this, MainActivity.class);
        this.startActivity(intent);
    }
}