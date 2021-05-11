package com.example.remotesoft_receives;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
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
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;


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

import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    Button btn_acercaDe, btn_configTS;
    Switch switchB;

    Button btn_pulsaciones, btn_ubicacion, btn_acelerometro;
    SharedPreferences.Editor editor;
    SharedPreferences sharedPreferences;
    //MQTT
    MqttAndroidClient client;
    String username;
    String MQTT_API_Key;
    String channelID;
    String READ_API_KEY;

    String textoJSON;
    JSONObject jsonObject;

    double latitud,longitud;
    int pulsaciones;
    double cor_x, cor_y,cor_z;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_acercaDe = findViewById(R.id.acerca_de);
        btn_acercaDe.setOnClickListener(v -> openAcercaDeActivity());

        btn_configTS = findViewById(R.id.configurar_TS);
        btn_configTS.setOnClickListener(v -> openConfigThingSpeak());

        btn_pulsaciones=findViewById(R.id.ver_Pulsaciones);
        btn_pulsaciones.setOnClickListener(v -> openVerPulsacionesActivity());

        btn_ubicacion=findViewById(R.id.ver_Ubicacion);
        btn_ubicacion.setOnClickListener(v -> openVerUbicacionActivity());

        btn_acelerometro=findViewById(R.id.ver_Acelerometro);
        btn_acelerometro.setOnClickListener(v -> openVerAcelerometroActivity());

        //Guardar estado switch
        switchB=findViewById(R.id.encender_app);
        sharedPreferences= getSharedPreferences("save",MODE_PRIVATE);
        switchB.setChecked(sharedPreferences.getBoolean("value", false));


        switchB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(switchB.isChecked()){
                    switchActivado();
                }else{
                    editor=getSharedPreferences("save",MODE_PRIVATE).edit();
                    editor.putBoolean("value",false);
                    editor.apply();
                    switchB.setChecked(false);
                    btn_pulsaciones.setVisibility(View.INVISIBLE);
                    btn_ubicacion.setVisibility(View.INVISIBLE);
                    btn_acelerometro.setVisibility(View.INVISIBLE);
                }
            }
        });

        if(switchB.isChecked()){
            switchActivado();
        }else{
            editor=getSharedPreferences("save",MODE_PRIVATE).edit();
            editor.putBoolean("value",false);
            editor.apply();
            switchB.setChecked(false);
            btn_pulsaciones.setVisibility(View.INVISIBLE);
            btn_ubicacion.setVisibility(View.INVISIBLE);
            btn_acelerometro.setVisibility(View.INVISIBLE);
        }

    }

    public void showNotification(Context context, String title, String message, Intent intent, int reqCode) {

        PendingIntent pendingIntent = PendingIntent.getActivity(context, reqCode, intent, PendingIntent.FLAG_ONE_SHOT);
        String CHANNEL_ID = "channel_name";// The id of the channel.
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.logo_app_round)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pendingIntent);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Channel Name";// The user-visible name of the channel.
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            notificationManager.createNotificationChannel(mChannel);
        }
        notificationManager.notify(reqCode, notificationBuilder.build()); // 0 is the request code, it should be unique id
    }

    public void openAcercaDeActivity(){
        Intent intent =new Intent(this, AcercaDe.class);
        startActivity(intent);
    }

    public void openVerPulsacionesActivity(){
        Intent intent = new Intent();
        intent.putExtra("pulsaciones", pulsaciones);
        intent.setClass(this, verPulsaciones.class);
        startActivity(intent);

    }

    public void openVerUbicacionActivity(){
        Intent intent = new Intent();
        intent.putExtra("latitud", latitud);
        intent.putExtra("longitud", longitud);
        intent.setClass(this, verUbicacion.class);
        startActivity(intent);

    }

    public void openVerAcelerometroActivity(){
        Intent intent = new Intent();
        intent.putExtra("cor_x", cor_x);
        intent.putExtra("cor_y", cor_y);
        intent.putExtra("cor_z", cor_z);
        intent.setClass(this, verAcelerometro.class);
        startActivity(intent);

    }

    private void switchActivado(){
        editor=getSharedPreferences("save",MODE_PRIVATE).edit();
        editor.putBoolean("value",true);
        editor.apply();
        switchB.setChecked(true);

        try {

            btn_pulsaciones.setVisibility(View.VISIBLE);
            btn_ubicacion.setVisibility(View.VISIBLE);
            btn_acelerometro.setVisibility(View.VISIBLE);

            channelID=sharedPreferences.getString("canal","");
            READ_API_KEY=sharedPreferences.getString("readKEY","");;
            MQTT_API_Key=sharedPreferences.getString("MQTTKey","");
            username=sharedPreferences.getString("username","");;

            if(channelID.equals("")|| READ_API_KEY.equals("")|| MQTT_API_Key.equals("") || username.equals("")){
                channelID="1362377";
                READ_API_KEY ="XU2DIK5NBQDQP1ME";
                MQTT_API_Key="OXEBXSCYAENX76QW";
                username="mwa0000022240279";
            }

            String clientId =MqttClient.generateClientId();
            client=new MqttAndroidClient(MainActivity.this.getApplicationContext(), "tcp://mqtt.thingspeak.com:1883", clientId);

            MqttConnectOptions options=new MqttConnectOptions();
            options.setUserName(username);
            options.setPassword(MQTT_API_Key.toCharArray());

            IMqttToken token = client.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Toast.makeText(MainActivity.this,"Conectado a ThingSpeak", Toast.LENGTH_SHORT).show();
                    try {
                        client.subscribe("channels/"+ channelID+"/subscribe/json/"+READ_API_KEY,0);

                        client.setCallback(new MqttCallback() {
                            @Override
                            public void connectionLost(Throwable cause) {

                            }

                            @Override
                            public void messageArrived(String topic, MqttMessage message) throws Exception {
                                textoJSON=new String(message.getPayload());
                                //Aqui hacer el parseo
                                jsonObject=new JSONObject(textoJSON);
                                pulsaciones=jsonObject.getInt("field1");

                                cor_x=jsonObject.getDouble("field2");
                                cor_y=jsonObject.getDouble("field3");
                                cor_z=jsonObject.getDouble("field4");

                                latitud=jsonObject.getDouble("field5");
                                longitud=jsonObject.getDouble("field6");

                                //Toast.makeText(MainActivity.this, pulsaciones + cor_x+ cor_y + cor_z + latitud + longitud, Toast.LENGTH_LONG).show();

                                if(pulsaciones >= 100){
                                    showNotification(MainActivity.this,"ALARMA", "Pulsaciones igual o superior a 100 LPM", new Intent(),0);
                                }



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
        builder.setTitle("Datos del canal a leer");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText username_Box = new EditText(this);
        username_Box.setHint("Username");
        layout.addView(username_Box);

        final EditText MQTT_API_Key_Box = new EditText(this);
        MQTT_API_Key_Box.setHint("MQTT_API_KEY");
        layout.addView(MQTT_API_Key_Box);

        final EditText channelID_Box = new EditText(this);
        channelID_Box.setHint("Channel ID");
        layout.addView(channelID_Box);

        final EditText READ_API_KEY_Box = new EditText(this);
        READ_API_KEY_Box.setHint("READ_API_KEY");
        layout.addView(READ_API_KEY_Box);

        builder.setView(layout);
        builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                username=username_Box.getText().toString();
                MQTT_API_Key=MQTT_API_Key_Box.getText().toString();
                channelID=channelID_Box.getText().toString();
                READ_API_KEY =READ_API_KEY_Box.getText().toString();


                editor=getSharedPreferences("save",MODE_PRIVATE).edit();
                editor.putString("canal",channelID_Box.getText().toString());
                editor.putString("readKEY",READ_API_KEY_Box.getText().toString());
                editor.putString("MQTTKey",MQTT_API_Key_Box.getText().toString());
                editor.putString("username",username_Box.getText().toString());
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