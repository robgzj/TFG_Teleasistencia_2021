package com.example.tfg_teleasistencia_2021;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity{

    Button btn_acercaDe, btn_datosSensores;
    Switch switchB;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_acercaDe = findViewById(R.id.acerca_de);
        btn_acercaDe.setOnClickListener(v -> openAcercaDeActivity());

        btn_datosSensores = findViewById(R.id.datos_sensores);
        btn_datosSensores.setOnClickListener(v -> openDatosActivity());

        //Guardar estado switch
        switchB=findViewById(R.id.encender_app);
        SharedPreferences sharedPreferences= getSharedPreferences("save",MODE_PRIVATE);
        switchB.setChecked(sharedPreferences.getBoolean("value", true));

        switchB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(switchB.isChecked()){
                    SharedPreferences.Editor editor=getSharedPreferences("save",MODE_PRIVATE).edit();
                    editor.putBoolean("value",true);
                    editor.apply();
                    switchB.setChecked(true);
                }else{
                    SharedPreferences.Editor editor=getSharedPreferences("save",MODE_PRIVATE).edit();
                    editor.putBoolean("value",false);
                    editor.apply();
                    switchB.setChecked(false);
                }
            }
        });
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