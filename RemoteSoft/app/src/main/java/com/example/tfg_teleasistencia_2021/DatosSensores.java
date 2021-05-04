package com.example.tfg_teleasistencia_2021;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.zhaoxiaodan.miband.ActionCallback;
import com.zhaoxiaodan.miband.MiBand;
import com.zhaoxiaodan.miband.listeners.HeartRateNotifyListener;
import com.zhaoxiaodan.miband.listeners.NotifyListener;
import com.zhaoxiaodan.miband.model.UserInfo;
import com.zhaoxiaodan.miband.model.VibrationMode;

import java.util.Arrays;

public class DatosSensores extends AppCompatActivity {

    Button btn_atras;
    TextView txtUbi, txtAcelerometro, txtPulsaciones;
    Sensor acelerometro;
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;
    protected LocationManager mLocationManager;
    protected SensorManager mSensorManager;
    TextView conectado_a2;

    private BluetoothDevice device;
    private MiBand miBand;


    protected LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            txtUbi.setText(String.format("Latitud: %s\nLongitud: %s", location.getLatitude(), location.getLongitude()));
        }
    };


    protected SensorEventListener mSensorListener= new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            txtAcelerometro.setText(String.format("X: %s\nY: %s\nZ: %s",event.values[0], event.values[1], event.values[2]));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    protected HeartRateNotifyListener mHealthListener= new HeartRateNotifyListener() {
        @Override
        public void onNotify(int heartRate) {
            txtPulsaciones.setText(heartRate + " LPM");
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.datos_layout);

        btn_atras= findViewById(R.id.boton_atras2);
        btn_atras.setOnClickListener(v -> openMainActivity());

        txtUbi=findViewById(R.id.datos_ubicacion);
        txtAcelerometro=findViewById(R.id.datos_acelerometro);
        txtPulsaciones=findViewById(R.id.datos_pulsaciones);

        Intent intent = this.getIntent();
        device = intent.getParcelableExtra("device");

        miBand= new MiBand(this);
        if(device!= null){
            conectar_dispositivo(miBand, device);
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    miBand.setHeartRateScanListener(mHealthListener);
                    calcular_pulsaciones();
                }
            }, 1000);
        }


        getAccelerometerValues();

        //Si no ha pedido permisos de ubicación los pide, si ya los ha pedido, no hace falta
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION);
        }else{
            getCurrentLocation();
        }

    }

    private void calcular_pulsaciones() {
            final Handler handler=new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    miBand.startHeartRateScan();
                    handler.postDelayed(this, 15000);
                }
            }, 0);
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

    public void openMainActivity(){
        Intent intent =new Intent();
        intent.putExtra("device", device);
        intent.setClass(this, MainActivity.class);
        this.startActivity(intent);
    }

    public void conectar_dispositivo(MiBand miBand, BluetoothDevice device){
        final ProgressDialog pd = ProgressDialog.show(this, "", "Conectando al dispositivo ...");
        miBand.connect(device, new ActionCallback() {

            @Override
            public void onSuccess(Object data) {
                //Toast.makeText(ConfirmarVinculacion.this, "Dispositivo conectado correctamente!", Toast.LENGTH_SHORT).show();
                pd.dismiss();
                conectado_a2=findViewById(R.id.conectado_a2);
                conectado_a2.setText("Conectado a: " + device.getName());

                UserInfo userInfo = new UserInfo(20271234, 1, 32, 180, 80, "Usuario", 0);
                miBand.setUserInfo(userInfo);


                miBand.setDisconnectedListener(new NotifyListener() {
                    @Override
                    public void onNotify(byte[] data) {

                    }
                });
            }

            @Override
            public void onFail(int errorCode, String msg) {
                pd.dismiss();
                //Toast.makeText(ConfirmarVinculacion.this, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
