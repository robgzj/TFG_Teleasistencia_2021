package com.example.tfg_teleasistencia_2021;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class DatosSensores extends AppCompatActivity {

    Button btn_atras;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.datos_layout);

        btn_atras= findViewById(R.id.boton_atras2);
        btn_atras.setOnClickListener(v -> openMainActivity());


    }
    public void openMainActivity(){
        Intent intent =new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
