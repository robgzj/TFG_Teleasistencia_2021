package com.example.remotesoft_receives;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MainActivity extends AppCompatActivity {

    Button btn_acercaDe, btn_configTS;
    Switch switchB;

    TextView infoTS;
    //MQTT
    MqttAndroidClient client;
    String username="mwa0000022240279";
    String MQTT_API_Key="OXEBXSCYAENX76QW";
    String channelID="1362377";
    String READ_API_KEY ="XU2DIK5NBQDQP1ME";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_acercaDe = findViewById(R.id.acerca_de);
        btn_acercaDe.setOnClickListener(v -> openAcercaDeActivity());


        btn_configTS = findViewById(R.id.configurar_TS);
        btn_configTS.setOnClickListener(v -> openConfigThingSpeak());

        infoTS= findViewById(R.id.info_TS);

        //Guardar estado switch
        switchB=findViewById(R.id.encender_app);
        SharedPreferences sharedPreferences= getSharedPreferences("save",MODE_PRIVATE);
        switchB.setChecked(sharedPreferences.getBoolean("value", false));

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




        switchB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    try {
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
                                            infoTS.setText(new String(message.getPayload()));
                                            //Aqui hacer el parseo
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
            }
        });
    }


    public void openAcercaDeActivity(){
        Intent intent =new Intent(this, AcercaDe.class);
        startActivity(intent);
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