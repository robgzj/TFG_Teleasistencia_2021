package com.example.remotesoft_receives;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class verPulsaciones extends AppCompatActivity {

    Button btn_atras;
    TextView valorPulsaciones;
    int pulsaciones;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ver_pulsaciones);

        btn_atras= findViewById(R.id.boton_atras);
        btn_atras.setOnClickListener(v -> openMainActivity());

        Intent intent=getIntent();
        pulsaciones=intent.getExtras().getInt("pulsaciones");
        valorPulsaciones= findViewById(R.id.datos_pulsaciones);
        valorPulsaciones.setText(Integer.toString(pulsaciones));
    }

    public void openMainActivity(){
        Intent intent =new Intent();
        intent.setClass(this, MainActivity.class);
        this.startActivity(intent);
    }
}