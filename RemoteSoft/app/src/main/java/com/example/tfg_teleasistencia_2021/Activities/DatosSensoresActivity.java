package com.example.tfg_teleasistencia_2021.Activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.tfg_teleasistencia_2021.Pulsera;
import com.example.tfg_teleasistencia_2021.R;
import com.example.tfg_teleasistencia_2021.DetectaCaida;
import com.zhaoxiaodan.miband.MiBand;
import com.zhaoxiaodan.miband.listeners.HeartRateNotifyListener;

public class DatosSensoresActivity extends AppCompatActivity {

    //Atributos utilizados en la vista
    private Button btn_atras;
    private TextView txtUbi, txtAcelerometro, txtPulsaciones;
    private TextView conectado_a;
    private TextView hayCaida;

    //Atributos para calcular la ubicacion
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;
    private LocationManager mLocationManager;

    //Atributos de la pulsera
    private BluetoothDevice device;
    private MiBand miBand;
    private Pulsera pulsera;

    //Atributos del acelerometro
    private Sensor acelerometro;
    private SensorManager mSensorManager;

    //Atributos para el detector de caidas
    private double ac;
    private DetectaCaida ventana;

    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            txtUbi.setText(String.format("Latitud: %s\nLongitud: %s", location.getLatitude(), location.getLongitude()));
        }
    };

    private SensorEventListener mSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            txtAcelerometro.setText(String.format("X: %s\nY: %s\nZ: %s", event.values[0], event.values[1], event.values[2]));

            ac = Math.sqrt(event.values[0] * event.values[0] + event.values[1] * event.values[1] + event.values[2] * event.values[2]);

            ventana.add(ac);

            if (ventana.isFull() && ventana.isFallDetected()) {
                hayCaida.setText("Si");
                ventana.clear();
            } else {
                hayCaida.setText("No");
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private HeartRateNotifyListener mHealthListener = new HeartRateNotifyListener() {
        @Override
        public void onNotify(int heartRate) {
            txtPulsaciones.setText(heartRate + " LPM");
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.datos_layout);

        btn_atras = findViewById(R.id.boton_atras2);
        btn_atras.setOnClickListener(v -> openMainActivity());

        txtUbi = findViewById(R.id.datos_ubicacion);
        txtAcelerometro = findViewById(R.id.datos_acelerometro);
        txtPulsaciones = findViewById(R.id.datos_pulsaciones);

        hayCaida = findViewById(R.id.hay_caida);
        conectado_a = findViewById(R.id.conectado_a2);

        Intent intent = this.getIntent();
        device = intent.getParcelableExtra("device");
        miBand = new MiBand(this);
        pulsera = new Pulsera(miBand, device);

        //Conservamos la conexion si hay dispositivo vinculado
        if (device != null) {
            pulsera.conectar_dispositivo(this);
            conectado_a.setText("Conectado a: " + device.getName());
            pulsera.calcular_pulsaciones(this, mHealthListener);
        }
        ventana = new DetectaCaida(10);
        getAccelerometerValues();

        //Si no ha pedido permisos de ubicación los pide, si ya los ha pedido, no hace falta
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION);
        } else {
            getCurrentLocation();
        }



    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //En los permisos de ubicacion, si ha dicho que si, te da la ubicacion, si no te manda un mensaje de permiso denegado
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
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
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        acelerometro = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(mSensorListener, acelerometro, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void openMainActivity() {
        pulsera.stopCalcularPulsaciones();
        super.onBackPressed();
        this.finish();
    }
}
