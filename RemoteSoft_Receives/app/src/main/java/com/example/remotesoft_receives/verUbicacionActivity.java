package com.example.remotesoft_receives;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

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

public class verUbicacionActivity extends  AppCompatActivity implements OnMapReadyCallback {

    //Atributos utilizados en la vista
    private Button btn_atras;

    //Por defecto mandara a las coordenadas 0 0
    private double latitud=0;
    private double longitud=0;

    //Atributos MQTT para suscribirse y recibir los datos de los sensores por ThingSpeak
    private MqttAndroidClient client;
    private String username;
    private String MQTT_API_Key;
    private String channelID;
    private String READ_API_KEY;

    //Atributos utilizados en el parseo JSON
    private String textoJSON;
    private JSONObject jsonObject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ver_ubicacion);

        btn_atras= findViewById(R.id.boton_atras);
        btn_atras.setOnClickListener(v -> openMainActivity());

        Intent intent=getIntent();
        channelID=intent.getExtras().getString("channelID");
        READ_API_KEY=intent.getExtras().getString("READ_API_KEY");
        MQTT_API_Key=intent.getExtras().getString("MQTT_API_Key");
        username=intent.getExtras().getString("username");

        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.google_map);

        try {
            String clientId = MqttClient.generateClientId();
            client = new MqttAndroidClient(verUbicacionActivity.this.getApplicationContext(), "tcp://mqtt.thingspeak.com:1883", clientId);

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
                                //Aqui hacer el parseo
                                jsonObject = new JSONObject(textoJSON);

                                latitud = jsonObject.getDouble("field5");
                                longitud = jsonObject.getDouble("field6");

                                supportMapFragment.getMapAsync(verUbicacionActivity.this);

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

    //Pone la ubicacion con su marcador
    @Override
    public void onMapReady(GoogleMap googleMap) {
        LatLng latLng=new LatLng(latitud, longitud);
        MarkerOptions markerOptions = new MarkerOptions().position(latLng).title("Ultima Posicion");
        googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,19));
        googleMap.addMarker(markerOptions);
    }
}