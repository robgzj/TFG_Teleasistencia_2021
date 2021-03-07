package com.example.tfg_teleasistencia_2021;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity{

    Button btn_acercaDe, btn_datosSensores;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_acercaDe = findViewById(R.id.acerca_de);
        btn_acercaDe.setOnClickListener(v -> openAcercaDeActivity());

        btn_datosSensores = findViewById(R.id.datos_sensores);
        btn_datosSensores.setOnClickListener(v -> openDatosActivity());
    }

    public void openAcercaDeActivity(){
        Intent intent =new Intent(this, AcercaDe.class);
        startActivity(intent);
    }

    public void openDatosActivity(){
        Intent intent =new Intent(this, DatosSensores.class);
        startActivity(intent);
    }

}