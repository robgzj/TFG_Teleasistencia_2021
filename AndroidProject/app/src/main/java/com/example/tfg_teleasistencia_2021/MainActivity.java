package com.example.tfg_teleasistencia_2021;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.zhaoxiaodan.miband.ActionCallback;
import com.zhaoxiaodan.miband.MiBand;
import com.zhaoxiaodan.miband.listeners.NotifyListener;
import com.zhaoxiaodan.miband.model.LedColor;
import com.zhaoxiaodan.miband.model.VibrationMode;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity{

    Button btn_acercaDe, btn_datosSensores, btn_vinculacion;
    Switch switchB;

    String valoresUbicacion;
    String valoresAcelerometro;

    private MiBand miBand;

    Sensor acelerometro;
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;
    protected LocationManager mLocationManager;
    protected SensorManager mSensorManager;

    protected LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            valoresUbicacion=(String.format("Latitud: %s\nLongitud: %s", location.getLatitude(), location.getLongitude()));
        }
    };


    protected SensorEventListener mSensorListener= new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            valoresAcelerometro=(String.format("X: %s\nY: %s\nZ: %s",event.values[0], event.values[1], event.values[2]));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_acercaDe = findViewById(R.id.acerca_de);
        btn_acercaDe.setOnClickListener(v -> openAcercaDeActivity());

        btn_datosSensores = findViewById(R.id.datos_sensores);
        btn_datosSensores.setOnClickListener(v -> openDatosActivity());

        btn_vinculacion = findViewById(R.id.vincular_pulsera);
        btn_vinculacion.setOnClickListener(v -> openVinculacionctivity());


        Intent intent=this.getIntent();

            final BluetoothDevice device = intent.getParcelableExtra("device");
            if(device!=null) {
                miBand = new MiBand(this);
                Toast.makeText(this, "Conectando a dispositivo " + device.getName(), Toast.LENGTH_SHORT).show();
                miBand.connect(device, new ActionCallback() {
                    @Override
                    public void onSuccess(Object data) {
                        Toast.makeText(MainActivity.this, "Dispositivo conectado correctamente!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFail(int errorCode, String msg) {
                        Toast.makeText(MainActivity.this, "Error de conexión!, code: " + errorCode + " msg: " + msg, Toast.LENGTH_SHORT).show();
                    }
                });
                miBand.pair(new ActionCallback() {
                    @Override
                    public void onSuccess(Object data) {
                        Toast.makeText(MainActivity.this, "Dispositivo vinculado correctamente!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFail(int errorCode, String msg) {
                        Toast.makeText(MainActivity.this, "Error de vinculación!", Toast.LENGTH_SHORT).show();
                    }
                });
                miBand.startVibration(VibrationMode.VIBRATION_WITH_LED);
            }
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

        getAccelerometerValues();

        //Si no ha pedido permisos de ubicación los pide, si ya los ha pedido, no hace falta
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION);
        }else{
            getCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //En los permisos de ubicacion, si ha dicho que si, te da la ubicacion, si no te manda un mensaje de permiso denegado
        if(requestCode==REQUEST_CODE_LOCATION_PERMISSION && grantResults.length >0){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                getCurrentLocation();
            }else{
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0,
                0, mLocationListener);
    }

    private void getAccelerometerValues() {
        mSensorManager= (SensorManager) getSystemService(SENSOR_SERVICE);

        acelerometro= mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(mSensorListener, acelerometro, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void openAcercaDeActivity(){
        Intent intent =new Intent(this, AcercaDe.class);
        startActivity(intent);
    }

    public void openDatosActivity(){
        Intent intent =new Intent(this, DatosSensores.class);
        startActivity(intent);
    }

    public void openVinculacionctivity(){
        Intent intent =new Intent(this, VinculacionPulsera.class);
        startActivity(intent);
    }


}