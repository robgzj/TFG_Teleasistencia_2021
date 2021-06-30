package com.example.tfg_teleasistencia_2021.Activities;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tfg_teleasistencia_2021.Pulsera;
import com.example.tfg_teleasistencia_2021.R;
import com.zhaoxiaodan.miband.MiBand;

public class AcercaDeActivity extends AppCompatActivity {

    private Button btn_atras;
    private TextView conectado_a;

    //Atributos de la pulsera
    private MiBand miBand;
    private BluetoothDevice device;
    private Pulsera pulsera;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acercade_layout);

        btn_atras = findViewById(R.id.boton_atras);
        btn_atras.setOnClickListener(v -> openMainActivity());

        conectado_a = findViewById(R.id.conectado_a3);

        Intent intent = this.getIntent();
        device = intent.getParcelableExtra("device");
        miBand = new MiBand(this);
        pulsera = new Pulsera(miBand, device);

        //Conservamos la conexion si hay dispositivo vinculado
        if (device != null) {
            pulsera.conectar_dispositivo(this);
            conectado_a.setText("Conectado a: " + device.getName());
        }

    }

    public void openMainActivity() {
        super.onBackPressed();
        this.finish();
    }
}
