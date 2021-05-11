package com.example.remotesoft_receives;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class verAcelerometro extends AppCompatActivity {

    Button btn_atras;
    TextView valor_cor_x, valor_cor_y, valor_cor_z;
    double cor_x, cor_y, cor_z;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ver_acelerometro);

        btn_atras= findViewById(R.id.boton_atras);
        btn_atras.setOnClickListener(v -> openMainActivity());

        Intent intent=getIntent();
        cor_x=intent.getExtras().getDouble("cor_x");
        cor_y=intent.getExtras().getDouble("cor_y");
        cor_z=intent.getExtras().getDouble("cor_z");

        valor_cor_x= findViewById(R.id.datos_cor_x);
        valor_cor_y= findViewById(R.id.datos_cor_y);
        valor_cor_z= findViewById(R.id.datos_cor_z);

        valor_cor_x.setText(Double.toString(cor_x));
        valor_cor_y.setText(Double.toString(cor_y));
        valor_cor_z.setText(Double.toString(cor_z));
    }

    public void openMainActivity(){
        Intent intent =new Intent();
        intent.setClass(this, MainActivity.class);
        this.startActivity(intent);
    }
}