package com.example.tfg_teleasistencia_2021;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

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

import com.zhaoxiaodan.miband.ActionCallback;
import com.zhaoxiaodan.miband.MiBand;
import com.zhaoxiaodan.miband.listeners.HeartRateNotifyListener;
import com.zhaoxiaodan.miband.listeners.NotifyListener;
import com.zhaoxiaodan.miband.model.UserInfo;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;

public class MainActivity extends AppCompatActivity{

    Button btn_acercaDe, btn_datosSensores, btn_vinculacion, btn_configTS;
    Switch switchB;

    String valorLatitud="0";
    String valorLongitud="0";
    String valorX="0";
    String valorY="0";
    String valorZ="0";
    String valoresPulsacion="0";
    TextView conectado_a;


    private MiBand miBand;
    private BluetoothDevice device;

    Sensor acelerometro;
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;
    protected LocationManager mLocationManager;
    protected SensorManager mSensorManager;

    //MQTT
    MqttAndroidClient client;
    String channelID="1362377";
    String WRITE_API_KEY ="Q38TDPXSWT30IT7T";

    protected LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            valorLatitud= location.getLatitude() + "";
            valorLongitud= location.getLongitude() + "";
        }
    };


    protected SensorEventListener mSensorListener= new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            valorX= event.values[0] +"";
            valorY= event.values[1] +"";
            valorZ= event.values[2] +"";
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    protected HeartRateNotifyListener mHealthListener= new HeartRateNotifyListener() {
        @Override
        public void onNotify(int heartRate) {
            valoresPulsacion=String.valueOf(heartRate);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_acercaDe = findViewById(R.id.acerca_de);
        btn_acercaDe.setOnClickListener(v -> openAcercaDeActivity());

        btn_datosSensores = findViewById(R.id.datos_sensores);
        btn_datosSensores.setOnClickListener(v -> openDatosActivity());

        btn_vinculacion = findViewById(R.id.vincular_pulsera);
        btn_vinculacion.setOnClickListener(v -> openVinculacionctivity());

        btn_configTS = findViewById(R.id.btn_configTS);
        btn_configTS.setOnClickListener(v -> openConfigThingSpeak());

        Intent intent = this.getIntent();
        device = intent.getParcelableExtra("device");
        
        miBand= new MiBand(this);
        if(device!= null) {
            conectar_dispositivo(miBand, device);

            new Handler().postDelayed(new Runnable() {
                public void run() {
                    miBand.setHeartRateScanListener(mHealthListener);
                    calcular_pulsaciones();
                }
            }, 1000);

        }

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


        getAccelerometerValues();

        //Si no ha pedido permisos de ubicación los pide, si ya los ha pedido, no hace falta
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION);
        }else{
            getCurrentLocation();
        }





        switchB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    try {
                        String clientId =MqttClient.generateClientId();
                        client=new MqttAndroidClient(MainActivity.this.getApplicationContext(), "tcp://mqtt.thingspeak.com:1883", clientId);
                        IMqttToken token = client.connect();
                        token.setActionCallback(new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                // We are connected
                                Toast.makeText(MainActivity.this,"Conectado a ThingSpeak", Toast.LENGTH_SHORT).show();
                                publish_topic("channels/"+channelID+"/publish/" + WRITE_API_KEY, "field1="+valoresPulsacion+"&field2="+valorX+"&field3="+valorY+"&field4="+valorZ+"&field5="+valorLatitud+"&field6="+valorLongitud);

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

    private void publish_topic(String topic, String msg){
            final Handler handler=new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    try {
                        client.publish(topic, msg.getBytes(), 0, false);
                        if(!switchB.isChecked()){
                           return;
                        }
                    } catch (MqttException e) {
                        Toast.makeText(MainActivity.this, "No se ha enviado la informacion", Toast.LENGTH_SHORT).show();
                    }
                    handler.postDelayed(this, 15000);
                }
            }, 15000);

    }
    private void calcular_pulsaciones() {
        final Handler handler=new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                miBand.startHeartRateScan();
                handler.postDelayed(this, 14000);
            }
        }, 14000);
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

    public void openAcercaDeActivity(){
        Intent intent =new Intent(this, AcercaDe.class);
        startActivity(intent);
    }

    public void openDatosActivity(){
        Intent intent =new Intent();
        intent.putExtra("device", device);
        intent.setClass(this, DatosSensores.class);
        this.startActivity(intent);
    }

    public void openVinculacionctivity(){
        Intent intent =new Intent(this, VinculacionPulsera.class);
        startActivity(intent);
    }

    private void openConfigThingSpeak() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Datos del canal de destino");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText channelID_Box = new EditText(this);
        channelID_Box.setHint("Channel ID");
        layout.addView(channelID_Box);

        final EditText WRITE_API_KEY_Box = new EditText(this);
        WRITE_API_KEY_Box.setHint("WRITE_API_KEY");
        layout.addView(WRITE_API_KEY_Box);

        builder.setView(layout);
        builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                channelID=channelID_Box.getText().toString();
                WRITE_API_KEY =WRITE_API_KEY_Box.getText().toString();
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

    public void conectar_dispositivo(MiBand miband, BluetoothDevice device){
        final ProgressDialog pd = ProgressDialog.show(this, "", "Conectando al dispositivo ...");
        miband.connect(device, new ActionCallback() {

            @Override
            public void onSuccess(Object data) {
                //Toast.makeText(ConfirmarVinculacion.this, "Dispositivo conectado correctamente!", Toast.LENGTH_SHORT).show();
                pd.dismiss();
                conectado_a=findViewById(R.id.conectado_a);
                conectado_a.setText("Conectado a: " + device.getName());

                UserInfo userInfo = new UserInfo(20271234, 1, 32, 180, 80, "Usuario", 0);
                miBand.setUserInfo(userInfo);


                miband.setDisconnectedListener(new NotifyListener() {
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