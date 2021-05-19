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

public class verAcelerometroActivity extends AppCompatActivity {

    protected Button btn_atras;
    protected TextView valor_cor_x, valor_cor_y, valor_cor_z, valor_hayCaida;
    protected double cor_x, cor_y, cor_z;
    protected boolean hayCaida;

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
        setContentView(R.layout.activity_ver_acelerometro);

        btn_atras = findViewById(R.id.boton_atras);
        btn_atras.setOnClickListener(v -> openMainActivity());

        Intent intent = getIntent();
        channelID=intent.getExtras().getString("channelID");
        READ_API_KEY=intent.getExtras().getString("READ_API_KEY");
        MQTT_API_Key=intent.getExtras().getString("MQTT_API_Key");
        username=intent.getExtras().getString("username");


        valor_cor_x = findViewById(R.id.datos_cor_x);
        valor_cor_y = findViewById(R.id.datos_cor_y);
        valor_cor_z = findViewById(R.id.datos_cor_z);
        valor_hayCaida = findViewById(R.id.datos_caida);

        try {
        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(verAcelerometroActivity.this.getApplicationContext(), "tcp://mqtt.thingspeak.com:1883", clientId);

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

                            cor_x = jsonObject.getDouble("field2");
                            cor_y = jsonObject.getDouble("field3");
                            cor_z = jsonObject.getDouble("field4");


                            if(jsonObject.getString("field7").equals("1")){
                                valor_hayCaida.setText("Si");

                            }else{
                                valor_hayCaida.setText("No");
                            }

                            valor_cor_x.setText(Double.toString(cor_x));
                            valor_cor_y.setText(Double.toString(cor_y));
                            valor_cor_z.setText(Double.toString(cor_z));
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

    public void openMainActivity() {
        super.onBackPressed();
        this.finish();
    }
}