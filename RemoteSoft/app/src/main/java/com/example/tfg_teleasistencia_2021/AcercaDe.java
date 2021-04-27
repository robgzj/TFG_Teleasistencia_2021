package com.example.tfg_teleasistencia_2021;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class AcercaDe extends AppCompatActivity {

    Button btn_atras;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acercade_layout);

        btn_atras= findViewById(R.id.boton_atras);
        btn_atras.setOnClickListener(v -> openMainActivity());


    }
    public void openMainActivity(){
        Intent intent =new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
