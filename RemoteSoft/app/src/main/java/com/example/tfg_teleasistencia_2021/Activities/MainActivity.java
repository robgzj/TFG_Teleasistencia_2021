package com.example.tfg_teleasistencia_2021.Activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.tfg_teleasistencia_2021.Pulsera;
import com.example.tfg_teleasistencia_2021.R;
import com.example.tfg_teleasistencia_2021.Window;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import com.zhaoxiaodan.miband.MiBand;
import com.zhaoxiaodan.miband.listeners.HeartRateNotifyListener;

public class MainActivity extends AppCompatActivity {

    private Button btn_acercaDe, btn_datosSensores, btn_vinculacion, btn_configTS;
    private Switch switchB;

    private double valorLatitud;
    private double valorLongitud;
    private double valorX;
    private double valorY;
    private double valorZ ;
    private String valoresPulsacion="0";
    private String hayCaida="0";
    private TextView conectado_a;

    private SharedPreferences.Editor editor;
    private SharedPreferences sharedPreferences;

    private MiBand miBand;
    private BluetoothDevice device;
    private Pulsera pulsera;
    private double ac;
    private Window ventana;


    private Sensor acelerometro;
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;
    private LocationManager mLocationManager;
    private SensorManager mSensorManager;

    //MQTT
    private MqttAndroidClient client;
    private String username;
    private String channelID;
    private String WRITE_API_KEY;

    private MqttAndroidClient clientConf;
    private String MQTT_API_Key;
    private String channelID_Conf;
    private String READ_API_KEY_Conf;

    private String textoJSON;
    private JSONObject jsonObject;
    private int estadoMonitorizacion;

    private Handler handlerPublish = new Handler();
    private Runnable runnablePublish= new Runnable() {
        public void run() {
            try {
                client.publish("channels/" + channelID + "/publish/" + WRITE_API_KEY, ("field1=" + valoresPulsacion + "&field2=" + valorX + "&field3=" + valorY + "&field4=" + valorZ + "&field5=" + valorLatitud + "&field6=" + valorLongitud + "&field7=" + hayCaida).getBytes(), 0, false);
                if(estadoMonitorizacion==1){
                    hayCaida="0";
                }
            } catch (MqttException e) {
                Toast.makeText(MainActivity.this, "No se ha enviado la informacion", Toast.LENGTH_SHORT).show();
            }
            handlerPublish.postDelayed(this, 20000);
        }
    };




    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            valorLatitud = location.getLatitude();
            valorLongitud = location.getLongitude();
        }
    };


    private SensorEventListener mSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            valorX = event.values[0];
            valorY = event.values[1];
            valorZ = event.values[2];

            ac = Math.sqrt(event.values[0] * event.values[0] + event.values[1] * event.values[1] + event.values[2] * event.values[2]);

            ventana.add(ac);

            if (ventana.isFull() && ventana.isFallDetected()) {
                hayCaida = "1";
                ventana.clear();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private HeartRateNotifyListener mHealthListener= new HeartRateNotifyListener() {
        @Override
        public void onNotify(int heartRate) {
            if(estadoMonitorizacion==1){
                valoresPulsacion = heartRate+"";
            }else if(estadoMonitorizacion == 0 && heartRate<100){
                    //No actualizar nada
            }else{
                valoresPulsacion = heartRate+"";
            }
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Guardar estado switch
        switchB = findViewById(R.id.encender_app);
        sharedPreferences = getSharedPreferences("save", MODE_PRIVATE);
        switchB.setChecked(sharedPreferences.getBoolean("value", false));

        btn_acercaDe = findViewById(R.id.acerca_de);
        btn_acercaDe.setOnClickListener(v -> openAcercaDeActivity());

        btn_datosSensores = findViewById(R.id.datos_sensores);
        btn_datosSensores.setOnClickListener(v -> openDatosActivity());

        btn_vinculacion = findViewById(R.id.vincular_pulsera);
        btn_vinculacion.setOnClickListener(v -> openVinculacionctivity());

        btn_configTS = findViewById(R.id.btn_configTS);
        btn_configTS.setOnClickListener(v -> openConfigThingSpeak());

        conectado_a = findViewById(R.id.conectado_a);

        ventana = new Window();

        Intent intent = this.getIntent();
        device = intent.getParcelableExtra("device");
        if (device != null) {
            miBand = new MiBand(this);
            pulsera = new Pulsera(miBand, device);
            if(!switchB.isChecked()){
                pulsera.conectar_dispositivo(this);
                conectado_a.setText("Conectado a: " + device.getName());
            }
        }


        getAccelerometerValues();

        //Si no ha pedido permisos de ubicación los pide, si ya los ha pedido, no hace falta
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION);
        } else {
            getCurrentLocation();
        }

        switchB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switchB.isChecked()) {
                    switchActivado();
                } else {
                    editor = getSharedPreferences("save", MODE_PRIVATE).edit();
                    editor.putBoolean("value", false);
                    editor.apply();
                    switchB.setChecked(false);
                    if(device !=null){
                        pulsera.stopCalcularPulsaciones();
                    }
                    handlerPublish.removeCallbacks(runnablePublish);

                    try {
                        clientConf.unsubscribe("channels/" + channelID_Conf + "/subscribe/json/" + READ_API_KEY_Conf);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }

                }
            }
        });
        if (switchB.isChecked()) {
            switchActivado();
        } else {
            editor = getSharedPreferences("save", MODE_PRIVATE).edit();
            editor.putBoolean("value", false);
            editor.apply();
            switchB.setChecked(false);
            if(device !=null){
                pulsera.stopCalcularPulsaciones();
            }
            handlerPublish.removeCallbacks(runnablePublish);
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

    public void openAcercaDeActivity() {
        Intent intent = new Intent();
        intent.putExtra("device", device);
        intent.setClass(this, AcercaDeActivity.class);
        this.startActivity(intent);
    }

    public void openDatosActivity() {
        Intent intent = new Intent();
        intent.putExtra("device", device);
        intent.setClass(this, DatosSensoresActivity.class);
        this.startActivity(intent);
    }

    public void openVinculacionctivity() {
        if(device !=null){
            pulsera.stopCalcularPulsaciones();
        }
        Intent intent = new Intent(this, VinculacionPulseraActivity.class);
        startActivity(intent);
        this.finish();
    }

    private void switchActivado() {
        editor = getSharedPreferences("save", MODE_PRIVATE).edit();
        editor.putBoolean("value", true);
        editor.apply();
        switchB.setChecked(true);

        if (device != null) {
            pulsera.calcular_pulsaciones(this, mHealthListener);
            conectado_a.setText("Conectado a: " + device.getName());
        }

        try {
            channelID = sharedPreferences.getString("canal", "");
            WRITE_API_KEY = sharedPreferences.getString("writeKey", "");
            username = sharedPreferences.getString("username", "");

            channelID_Conf = sharedPreferences.getString("canalConf", "");
            MQTT_API_Key = sharedPreferences.getString("MQTTKey", "");
            READ_API_KEY_Conf = sharedPreferences.getString("readKeyConf", "");

            if (channelID.equals("")  || MQTT_API_Key.equals("") || username.equals("") || WRITE_API_KEY.equals("") || READ_API_KEY_Conf.equals("")|| channelID_Conf.equals("") ) {
                channelID = "1362377";
                username = "mwa0000022240279";
                WRITE_API_KEY = "Q38TDPXSWT30IT7T";
                MQTT_API_Key = "OXEBXSCYAENX76QW";

                READ_API_KEY_Conf = "SHB808A21438C1UA";
                channelID_Conf = "1402766";

            }
            //Conectamos al cliente de enviar datos
            String clientId = MqttClient.generateClientId();
            client = new MqttAndroidClient(MainActivity.this.getApplicationContext(), "tcp://mqtt.thingspeak.com:1883", clientId);
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    handlerPublish.postDelayed(runnablePublish, 20000);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems

                    Toast.makeText(MainActivity.this, "Error de conexion a TS", Toast.LENGTH_SHORT).show();
                }
            });

            //Conectamos al cliente de recibir la confirmacion
            String clientId_Conf = MqttClient.generateClientId();
            clientConf = new MqttAndroidClient(MainActivity.this.getApplicationContext(), "tcp://mqtt.thingspeak.com:1883", clientId_Conf);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(username);
            options.setPassword(MQTT_API_Key.toCharArray());

            IMqttToken token_conf = clientConf.connect(options);

            token_conf.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    try {
                        clientConf.subscribe("channels/" + channelID_Conf + "/subscribe/json/" + READ_API_KEY_Conf, 0);

                        clientConf.setCallback(new MqttCallback() {
                            @Override
                            public void connectionLost(Throwable cause) {

                            }

                            @Override
                            public void messageArrived(String topic, MqttMessage message) throws Exception {
                                textoJSON = new String(message.getPayload());
                                //Aqui hacer el parseo
                                jsonObject = new JSONObject(textoJSON);
                                estadoMonitorizacion = jsonObject.getInt("field1");
                            }

                            @Override
                            public void deliveryComplete(IMqttDeliveryToken token) {

                            }
                        });
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Toast.makeText(MainActivity.this, "Error de conexion a TS", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (MqttException e) {
        e.printStackTrace();
    }




    }

    private void openConfigThingSpeak() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Datos del canal de destino");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText username_Box = new EditText(this);
        username_Box.setHint("Username");
        layout.addView(username_Box);


        final EditText channelID_Box = new EditText(this);
        channelID_Box.setHint("Channel ID");
        layout.addView(channelID_Box);

        final EditText MQTT_API_Key_Box = new EditText(this);
        MQTT_API_Key_Box.setHint("MQTT_API_KEY");
        layout.addView(MQTT_API_Key_Box);

        final EditText WRITE_API_KEY_Box = new EditText(this);
        WRITE_API_KEY_Box.setHint("WRITE_API_KEY");
        layout.addView(WRITE_API_KEY_Box);


        final EditText channelID_Conf_Box = new EditText(this);
        channelID_Conf_Box.setHint("Channel ID Confirmación");
        layout.addView(channelID_Conf_Box);

        final EditText READ_API_KEY_Conf_Box = new EditText(this);
        READ_API_KEY_Conf_Box.setHint("READ_API_KEY Confirmación");
        layout.addView(READ_API_KEY_Conf_Box);




        builder.setView(layout);
        builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                editor = getSharedPreferences("save", MODE_PRIVATE).edit();
                editor.putString("canal", channelID_Box.getText().toString());
                editor.putString("writeKey", WRITE_API_KEY_Box.getText().toString());
                editor.putString("username", username_Box.getText().toString());

                editor.putString("canalConf", channelID_Conf_Box.getText().toString());
                editor.putString("MQTTKey", MQTT_API_Key_Box.getText().toString());
                editor.putString("readKeyConf", READ_API_KEY_Conf_Box.getText().toString());

                editor.apply();
                switchB.setChecked(false);
            }
        });

        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //No hace nada simplemente vuelve
            }
        });
        AlertDialog confirmacion = builder.create();
        confirmacion.show();
    }
}