package com.example.tfg_teleasistencia_2021.Activities;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tfg_teleasistencia_2021.R;
import com.zhaoxiaodan.miband.MiBand;

import java.util.ArrayList;
import java.util.HashMap;

public class VinculacionPulseraActivity extends AppCompatActivity {

    private Button btn_atras;
    private ListView listaDispositivos;
    private Switch switch_scan;

    private ScanCallback scanCallback;

    private HashMap<String, BluetoothDevice> devices = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vinculacion_layout);

        btn_atras = findViewById(R.id.boton_atras3);
        btn_atras.setOnClickListener(v -> openMainActivity());

        switch_scan = findViewById(R.id.switch_scan);
        listaDispositivos = findViewById(R.id.lista_dispositivos);

        final ArrayAdapter adapter = new ArrayAdapter<String>(this, R.layout.item, new ArrayList<>());

        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                BluetoothDevice device = result.getDevice();

                String item = device.getName() + " | " + device.getAddress();
                if (!devices.containsKey(item)) {
                    devices.put(item, device);
                    adapter.add(item);
                }

            }
        };

        switch_scan.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    MiBand.startScan(scanCallback);
                } else {
                    //Dejamos de buscar y borramos la lista de dispositivos
                    MiBand.stopScan(scanCallback);
                    adapter.clear();
                    devices.clear();

                }
            }
        });


        listaDispositivos.setAdapter(adapter);
        listaDispositivos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item = ((TextView) view).getText().toString();
                if (devices.containsKey(item)) {
                    MiBand.stopScan(scanCallback);

                    BluetoothDevice device = devices.get(item);

                    //Cogemos el dispositivo que se ha seleccionado y lo enviamos a la vista principal
                    Intent intent = new Intent();
                    intent.putExtra("device", device);
                    intent.setClass(VinculacionPulseraActivity.this, MainActivity.class);
                    VinculacionPulseraActivity.this.startActivity(intent);
                    VinculacionPulseraActivity.this.finish();
                }
            }
        });

    }

    public void openMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        this.finish();
    }
}
