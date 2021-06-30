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
import android.os.PowerManager;
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
import com.example.tfg_teleasistencia_2021.DetectaCaida;

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

    //Atributos utilizados en la vista
    private Button btn_acercaDe, btn_datosSensores, btn_vinculacion, btn_configTS;
    private Switch switchB;
    private TextView conectado_a;

    //Valores a enviar por ThingSpeak
    private double valorLatitud;
    private double valorLongitud;
    private double valorX;
    private double valorY;
    private double valorZ ;
    private String valoresPulsacion="0";
    private String hayCaida="0";

    //Atributos del editor para guardar los datos de forma persistente
    private SharedPreferences.Editor editor;
    private SharedPreferences sharedPreferences;

    //Atributos para el control de la pulsera
    private MiBand miBand;
    private BluetoothDevice device;
    private Pulsera pulsera;

    //Atributos utilizados para la deteccion de caida
    private double ac;
    private DetectaCaida ventana;

    //Atributos del acelerometro
    private SensorManager mSensorManager;
    private Sensor acelerometro;

    //Atributos para calcular la  ubicacion
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;
    private LocationManager mLocationManager;

    //Atributos MQTT para el envio de datos por ThingSpeak
    private MqttAndroidClient client;
    private String username;
    private String channelID;
    private String WRITE_API_KEY;

    //Atributos MQTT para suscribirse y comprobar si hay monitorizacion por ThingSpeak
    private MqttAndroidClient clientConf;
    private String MQTT_API_Key;
    private String channelID_Conf;
    private String READ_API_KEY_Conf;

    //Atributo de monitorizacion, 1 si esta monitorizando 0 si no
    private int estadoMonitorizacion;

    //Atributos utilizados en el parseo JSON
    private String textoJSON;
    private JSONObject jsonObject;

    private PowerManager.WakeLock wakeLock;

    private Handler handlerPublish = new Handler();

    //Runnable del envio de los datos a ThingSpeak periodico
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

    //Listener de la ubicacion
    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            valorLatitud = location.getLatitude();
            valorLongitud = location.getLongitude();
        }
    };

    // Listener del acelerometro
    private SensorEventListener mSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            valorX = event.values[0];
            valorY = event.values[1];
            valorZ = event.values[2];

            //Calculo del modulo de aceleracion
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

    //Listener de las pulsaciones
    private HeartRateNotifyListener mHealthListener= new HeartRateNotifyListener() {
        @Override
        public void onNotify(int heartRate) {
            if(estadoMonitorizacion==1){
                valoresPulsacion = heartRate+"";
            }else if(estadoMonitorizacion == 0 && heartRate<100){
                    //No actualizar nada, no es situacion de peligro mientras no se monitoriza
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

        //Creacion de la ventana del algoritmo de caida, por defecto a 10 valores
        ventana = new DetectaCaida();

        //Detecta si se ha pasado una pulsera, si lo detecta se conecta
        Intent intent = this.getIntent();
        device = intent.getParcelableExtra("device");
        if (device != null) {
            miBand = new MiBand(this);
            pulsera = new Pulsera(miBand, device);
            //Si el switch esta desactivado solo se conecta, si esta activado se conecta y empieza a calcular pulsaciones, se hace más adelante eso
            if(!switchB.isChecked()){
                pulsera.conectar_dispositivo(this);
                conectado_a.setText("Conectado a: " + device.getName());
            }
        }

        //Inicializamos el listener del acelerometro
        getAccelerometerValues();

        //Si no ha pedido permisos de ubicación los pide, si ya los ha pedido, no hace falta
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION);
        } else {
            //Inicializamos el listener de la ubicacion
            getCurrentLocation();
        }

        //Listener que se activa al clickar al switch
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
                    //Se guarda de manera persistente el estado del switch

                    //Si hay pulsera conectada deja de calcular pulsaciones
                    if(device !=null){
                        pulsera.stopCalcularPulsaciones();
                    }
                    handlerPublish.removeCallbacks(runnablePublish);

                    //Dejamos la suscripcion
                    try {
                        clientConf.unsubscribe("channels/" + channelID_Conf + "/subscribe/json/" + READ_API_KEY_Conf);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                    //Si dejamos de enviar liberamos el wakelock
                    wakeLock.release();
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

            //Por precaucion dejamos de calcular pulsaciones
            if(device !=null){
                pulsera.stopCalcularPulsaciones();
            }
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
    //Se desconecta la pulsera finalizando la actividad
    public void openVinculacionctivity() {
        if(device !=null){
            pulsera.stopCalcularPulsaciones();
        }
        Intent intent = new Intent(this, VinculacionPulseraActivity.class);
        startActivity(intent);
        this.finish();
    }
    //Se ejecutara este metodo al encender el switch
    private void switchActivado() {
        //Al dar al switch adquirimos el wakelock
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyApp::MyWakelockTag");
        wakeLock.acquire();

        editor = getSharedPreferences("save", MODE_PRIVATE).edit();
        editor.putBoolean("value", true);
        editor.apply();
        switchB.setChecked(true);

        if (device != null) {
            pulsera.calcular_pulsaciones(this, mHealthListener);
            conectado_a.setText("Conectado a: " + device.getName());
        }

        try {
            //Obtemos los datos de los canales de ThingSpeak que estan guardados de manera persistente
            channelID = sharedPreferences.getString("canal", "");
            WRITE_API_KEY = sharedPreferences.getString("writeKey", "");
            username = sharedPreferences.getString("username", "");

            channelID_Conf = sharedPreferences.getString("canalConf", "");
            MQTT_API_Key = sharedPreferences.getString("MQTTKey", "");
            READ_API_KEY_Conf = sharedPreferences.getString("readKeyConf", "");

            //En el caso que no se hayan introducido ningun dato o esten incorrectos se pone por defecto a mis canales, cambiar en versiones futuras
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
                    // Si conectamos ejecutamos la actividad periodica de enviar cada 20 segundos
                    handlerPublish.postDelayed(runnablePublish, 20000);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    //Si da error se notifica
                    Toast.makeText(MainActivity.this, "Error de conexion a TS", Toast.LENGTH_SHORT).show();
                }
            });

            //Conectamos al cliente de recibir la si hay alguien monitorizando
            String clientId_Conf = MqttClient.generateClientId();
            clientConf = new MqttAndroidClient(MainActivity.this.getApplicationContext(), "tcp://mqtt.thingspeak.com:1883", clientId_Conf);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(username);
            options.setPassword(MQTT_API_Key.toCharArray());

            IMqttToken token_conf = clientConf.connect(options);

            token_conf.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

                    try {
                        // Si conectamos se suscribe
                        clientConf.subscribe("channels/" + channelID_Conf + "/subscribe/json/" + READ_API_KEY_Conf, 0);

                        clientConf.setCallback(new MqttCallback() {
                            @Override
                            public void connectionLost(Throwable cause) {

                            }

                            @Override
                            public void messageArrived(String topic, MqttMessage message) throws Exception {
                                textoJSON = new String(message.getPayload());
                                //Aqui hacer el parseo de cada mensaje llegado
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
                    Toast.makeText(MainActivity.this, "Error de conexion a TS", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (MqttException e) {
        e.printStackTrace();
    }

    }
    //Metodo que sirve para configurar los datos de los canales de ThingSpeak
    private void openConfigThingSpeak() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Datos de los canales");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        //Añadimos los campos uno a uno
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
        channelID_Conf_Box.setHint("Channel ID Monitorización");
        layout.addView(channelID_Conf_Box);

        final EditText READ_API_KEY_Conf_Box = new EditText(this);
        READ_API_KEY_Conf_Box.setHint("READ_API_KEY Monitorización");
        layout.addView(READ_API_KEY_Conf_Box);


        builder.setView(layout);
        builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Modificamos los datos persistentes de los canales de ThingSpeak
                editor = getSharedPreferences("save", MODE_PRIVATE).edit();
                editor.putString("canal", channelID_Box.getText().toString());
                editor.putString("writeKey", WRITE_API_KEY_Box.getText().toString());
                editor.putString("username", username_Box.getText().toString());

                editor.putString("canalConf", channelID_Conf_Box.getText().toString());
                editor.putString("MQTTKey", MQTT_API_Key_Box.getText().toString());
                editor.putString("readKeyConf", READ_API_KEY_Conf_Box.getText().toString());
                editor.apply();

                //El switch desactivado para poder aplicar los cambios
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