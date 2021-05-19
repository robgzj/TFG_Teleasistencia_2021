package com.example.remotesoft_receives;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

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

public class verPulsacionesActivity extends AppCompatActivity {

    protected Button btn_atras;
    protected TextView valorPulsaciones;
    protected int pulsaciones;

    //MQTT
    protected MqttAndroidClient client;
    protected String username;
    protected String MQTT_API_Key;
    protected String channelID;
    protected String READ_API_KEY;

    protected String textoJSON;
    protected JSONObject jsonObject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ver_pulsaciones);

        btn_atras= findViewById(R.id.boton_atras);
        btn_atras.setOnClickListener(v -> openMainActivity());

        Intent intent=getIntent();
        channelID=intent.getExtras().getString("channelID");
        READ_API_KEY=intent.getExtras().getString("READ_API_KEY");
        MQTT_API_Key=intent.getExtras().getString("MQTT_API_Key");
        username=intent.getExtras().getString("username");
        valorPulsaciones= findViewById(R.id.datos_pulsaciones);

        try {
            String clientId = MqttClient.generateClientId();
            client = new MqttAndroidClient(this.getApplicationContext(), "tcp://mqtt.thingspeak.com:1883", clientId);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(username);
            options.setPassword(MQTT_API_Key.toCharArray());

            IMqttToken token = client.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    try {
                        client.subscribe("channels/" + channelID + "/subscribe/json/" + READ_API_KEY, 0);

                        client.setCallback(new MqttCallback() {
                            @Override
                            public void connectionLost(Throwable cause) {

                            }

                            @Override
                            public void messageArrived(String topic, MqttMessage message) throws Exception {
                                textoJSON = new String(message.getPayload());
                                //Aqui hacer el parseo
                                jsonObject = new JSONObject(textoJSON);

                                pulsaciones = jsonObject.getInt("field1");

                                valorPulsaciones.setText(Integer.toString(pulsaciones));
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
                }
            });


        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void openMainActivity(){
        super.onBackPressed();
        this.finish();
    }
}