package com.example.remotesoft_receives;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;


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

public class MainActivity extends AppCompatActivity {

    //Atributos utilizados en la vista
    private Button btn_acercaDe, btn_configTS;
    private Switch switchB;
    private Button btn_pulsaciones, btn_ubicacion, btn_acelerometro;

    //Atributos del editor para guardar los datos de forma persistente
    private SharedPreferences.Editor editor;
    private SharedPreferences sharedPreferences;

    //Atributos MQTT para suscribirse y recibir los datos de los sensores por ThingSpeak
    private MqttAndroidClient client;
    private String username;
    private String MQTT_API_Key;
    private String channelID;
    private String READ_API_KEY;

    //Atributos MQTT para el envio de la señal de monitorizacion por ThingSpeak
    private MqttAndroidClient clientConf;
    private String channelID_Conf;
    private String WRITE_API_KEY_Conf;

    //Atributos utilizados en el parseo JSON
    private String textoJSON;
    private JSONObject jsonObject;

    //Atributos que nos dicen si suena alguna alarma
    private String pulsaciones, caida;
    private boolean hayCaida;

    private PowerManager.WakeLock wakeLock;

    private Handler handlerPublish = new Handler();

    //Runnable que envia una señal de monitorizacion cada 15 segundos
    private Runnable runnablePublish= new Runnable() {
        public void run() {
            try {
                clientConf.publish("channels/" + channelID_Conf + "/publish/fields/field1/" + WRITE_API_KEY_Conf, ("1").getBytes(), 0, false);
            } catch (MqttException e) {
                Toast.makeText(MainActivity.this, "No se ha enviado la informacion", Toast.LENGTH_SHORT).show();
            }
            handlerPublish.postDelayed(this, 15000);
        }
    };
    //Runnable que envia una señal de que no se esta monitorizando
    private Runnable runnableStopMonitorizacion= new Runnable() {
        public void run() {
            try {
                clientConf.publish("channels/" + channelID_Conf + "/publish/fields/field1/" + WRITE_API_KEY_Conf, ("0").getBytes(), 0, false);

            } catch (MqttException e) {
                Toast.makeText(MainActivity.this, "No se ha enviado la informacion", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_acercaDe = findViewById(R.id.acerca_de);
        btn_acercaDe.setOnClickListener(v -> openAcercaDeActivity());

        btn_configTS = findViewById(R.id.configurar_TS);
        btn_configTS.setOnClickListener(v -> openConfigThingSpeak());

        btn_pulsaciones = findViewById(R.id.ver_Pulsaciones);
        btn_pulsaciones.setOnClickListener(v -> openVerPulsacionesActivity());

        btn_ubicacion = findViewById(R.id.ver_Ubicacion);
        btn_ubicacion.setOnClickListener(v -> openVerUbicacionActivity());

        btn_acelerometro = findViewById(R.id.ver_Acelerometro);
        btn_acelerometro.setOnClickListener(v -> openVerAcelerometroActivity());

        //Guardar estado switch
        switchB = findViewById(R.id.encender_app);
        sharedPreferences = getSharedPreferences("save", MODE_PRIVATE);
        switchB.setChecked(sharedPreferences.getBoolean("value", false));


        switchB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switchB.isChecked()) {
                    //Paramos el envio de la señal de no monitorizacion por si se estaba mandando en ese momento
                    handlerPublish.removeCallbacks(runnableStopMonitorizacion);
                    switchActivado();
                } else {
                    editor = getSharedPreferences("save", MODE_PRIVATE).edit();
                    editor.putBoolean("value", false);
                    editor.apply();
                    switchB.setChecked(false);

                    btn_pulsaciones.setVisibility(View.INVISIBLE);
                    btn_ubicacion.setVisibility(View.INVISIBLE);
                    btn_acelerometro.setVisibility(View.INVISIBLE);

                    //Paramos el envio de la señal de monitorizacion
                    handlerPublish.removeCallbacks(runnablePublish);

                    handlerPublish.postDelayed(runnableStopMonitorizacion,15000);
                    try {
                        client.unsubscribe("channels/" + channelID + "/subscribe/json/" + READ_API_KEY);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }

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

            btn_pulsaciones.setVisibility(View.INVISIBLE);
            btn_ubicacion.setVisibility(View.INVISIBLE);
            btn_acelerometro.setVisibility(View.INVISIBLE);
        }

    }

    public void showNotification(String title, String message, int reqCode) {

        PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, reqCode, new Intent(), PendingIntent.FLAG_ONE_SHOT);
        String CHANNEL_ID = "RS-Channel";
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(MainActivity.this, CHANNEL_ID)
                .setSmallIcon(R.color.design_default_color_error)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pendingIntent);
        NotificationManager notificationManager = (NotificationManager) MainActivity.this.getSystemService(Context.NOTIFICATION_SERVICE);

        CharSequence name = "RS-Channel";
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
        notificationManager.createNotificationChannel(mChannel);

        notificationManager.notify(reqCode, notificationBuilder.build());
    }

    public void openAcercaDeActivity() {
        Intent intent = new Intent(this, AcercaDeActivity.class);
        startActivity(intent);
    }
    //Enviamos a las actividades los datos del usuario a monitorizar
    public void openVerPulsacionesActivity() {
        Intent intent = new Intent();
        intent.putExtra("channelID", channelID);
        intent.putExtra("READ_API_KEY", READ_API_KEY);
        intent.putExtra("MQTT_API_Key", MQTT_API_Key);
        intent.putExtra("username", username);
        intent.setClass(this, verPulsacionesActivity.class);
        startActivity(intent);

    }

    public void openVerUbicacionActivity() {
        Intent intent = new Intent();
        intent.putExtra("channelID", channelID);
        intent.putExtra("READ_API_KEY", READ_API_KEY);
        intent.putExtra("MQTT_API_Key", MQTT_API_Key);
        intent.putExtra("username", username);
        intent.setClass(this, verUbicacionActivity.class);
        startActivity(intent);

    }

    public void openVerAcelerometroActivity() {
        Intent intent = new Intent();
        intent.putExtra("channelID", channelID);
        intent.putExtra("READ_API_KEY", READ_API_KEY);
        intent.putExtra("MQTT_API_Key", MQTT_API_Key);
        intent.putExtra("username", username);
        intent.setClass(this, verAcelerometroActivity.class);
        startActivity(intent);

    }

    private void switchActivado() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyApp::MyWakelockTag");
        wakeLock.acquire();

        editor = getSharedPreferences("save", MODE_PRIVATE).edit();
        editor.putBoolean("value", true);
        editor.apply();
        switchB.setChecked(true);

        try {

            btn_pulsaciones.setVisibility(View.VISIBLE);
            btn_ubicacion.setVisibility(View.VISIBLE);
            btn_acelerometro.setVisibility(View.VISIBLE);

            channelID = sharedPreferences.getString("canal", "");
            READ_API_KEY = sharedPreferences.getString("readKey", "");
            MQTT_API_Key = sharedPreferences.getString("MQTTKey", "");
            username = sharedPreferences.getString("username", "");

            channelID_Conf = sharedPreferences.getString("canalConf", "");
            WRITE_API_KEY_Conf = sharedPreferences.getString("writeKeyConf", "");

            if (channelID.equals("")  || MQTT_API_Key.equals("") || username.equals("") ||  READ_API_KEY.equals("")|| channelID_Conf.equals("") || WRITE_API_KEY_Conf.equals("")) {
                channelID = "1362377";
                MQTT_API_Key = "OXEBXSCYAENX76QW";
                username = "mwa0000022240279";
                READ_API_KEY = "Q38TDPXSWT30IT7T";

                WRITE_API_KEY_Conf = "889R8GB3XCUUCH1Z";
                channelID_Conf = "1402766";
            }
            //Cliente de recepcion de datos de los sensores
            String clientId = MqttClient.generateClientId();
            client = new MqttAndroidClient(MainActivity.this.getApplicationContext(), "tcp://mqtt.thingspeak.com:1883", clientId);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(username);
            options.setPassword(MQTT_API_Key.toCharArray());

            IMqttToken token = client.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

                    try {
                        client.subscribe("channels/" + channelID + "/subscribe/json/" + READ_API_KEY, 0);
                        client.setCallback(new MqttCallback() {
                            @Override
                            public void connectionLost(Throwable cause) {

                            }

                            @Override
                            public void messageArrived(String topic, MqttMessage message) throws Exception {
                                    textoJSON = new String(message.getPayload());
                                    //Aqui hacemos el parseo
                                    jsonObject = new JSONObject(textoJSON);
                                    pulsaciones = jsonObject.getString("field1");

                                    caida = jsonObject.getString("field7");

                                    if (caida.equals("1")) {
                                        hayCaida = true;
                                    } else {
                                        hayCaida = false;
                                    }

                                    //Creamos las notificaciones de las alarmas
                                    if (Integer.valueOf(pulsaciones) >= 100) {
                                        showNotification("ALARMA", "Pulsaciones igual o superior a 100 LPM",  0);
                                    }

                                    if (hayCaida) {
                                        showNotification( "ALARMA", "Caída detectada", 1);
                                    }


                            }

                            @Override
                            public void deliveryComplete(IMqttDeliveryToken token) {

                            }
                        });

                        //Cliente del envio de la señal de monitorizacion
                        String clientId_Conf = MqttClient.generateClientId();
                        clientConf = new MqttAndroidClient(MainActivity.this.getApplicationContext(), "tcp://mqtt.thingspeak.com:1883", clientId_Conf);
                        IMqttToken tokenConf = clientConf.connect();
                        tokenConf.setActionCallback(new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                handlerPublish.postDelayed(runnablePublish, 0);
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

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(MainActivity.this, "Error de conexion a TS", Toast.LENGTH_SHORT).show();
                }
            });


        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    //Ventana para configurar los canales de ThingSpeak a usar.
    private void openConfigThingSpeak() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Datos de los canales");

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

        final EditText READ_API_KEY_Box = new EditText(this);
        READ_API_KEY_Box.setHint("READ_API_KEY");
        layout.addView(READ_API_KEY_Box);


        final EditText channelID_Conf_Box = new EditText(this);
        channelID_Conf_Box.setHint("Channel ID Monitorización");
        layout.addView(channelID_Conf_Box);


        final EditText WRITE_API_KEY_Conf_Box = new EditText(this);
        WRITE_API_KEY_Conf_Box.setHint("WRITE_API_KEY Monitorización");
        layout.addView(WRITE_API_KEY_Conf_Box);

        builder.setView(layout);
        builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                editor = getSharedPreferences("save", MODE_PRIVATE).edit();
                editor.putString("canal", channelID_Box.getText().toString());
                editor.putString("readKey", READ_API_KEY_Box.getText().toString());
                editor.putString("MQTTKey", MQTT_API_Key_Box.getText().toString());
                editor.putString("username", username_Box.getText().toString());

                editor.putString("canalConf", channelID_Conf_Box.getText().toString());
                editor.putString("writeKeyConf", WRITE_API_KEY_Conf_Box.getText().toString());

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