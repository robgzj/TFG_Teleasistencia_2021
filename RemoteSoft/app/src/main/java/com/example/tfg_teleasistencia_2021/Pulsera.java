package com.example.tfg_teleasistencia_2021;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;

import com.zhaoxiaodan.miband.ActionCallback;
import com.zhaoxiaodan.miband.MiBand;
import com.zhaoxiaodan.miband.listeners.HeartRateNotifyListener;
import com.zhaoxiaodan.miband.listeners.NotifyListener;
import com.zhaoxiaodan.miband.model.UserInfo;

public class Pulsera {

    private MiBand miBand;
    private BluetoothDevice device;

    private Handler handler=new Handler();

    //Runnable de lectura de pulsaciones cada 14 segundos
    private Runnable myRunnable= new Runnable() {
        public void run() {
            miBand.startHeartRateScan();
            handler.postDelayed(this, 14000);
        }
    };
    //Atributo que comprueba si la pulsera esta conectada a la app
    private boolean isConnected;

    private ProgressDialog pd;

    public Pulsera(MiBand miBand, BluetoothDevice device) {
        this.miBand = miBand;
        this.device = device;
    }

    //Metodo que conecta la pulsera al dispositivo
    public void conectar_dispositivo(Context ctx) {
        pd = ProgressDialog.show(ctx, "", "Conectando al dispositivo ...");
        miBand.connect(device, new ActionCallback() {

            @Override
            public void onSuccess(Object data) {

                //Introducimos la informacion de usuario, como se ha comentado es irrelevante por lo que introducimos valores aleatorios
                UserInfo userInfo = new UserInfo(20271234, 1, 32, 180, 80, "Usuario", 0);
                miBand.setUserInfo(userInfo);

                isConnected=true;

                //Cuando se conecta quitamos la ventana del progreso
                pd.dismiss();

                miBand.setDisconnectedListener(new NotifyListener() {
                    @Override
                    public void onNotify(byte[] data) {

                    }
                });
            }

            @Override
            public void onFail(int errorCode, String msg) {
                pd.dismiss();
            }
        });
    }

    //Metodo de calcular de pulsaciones, si no esta conectado se conecta y luego calcula
    public void calcular_pulsaciones(Context ctx, HeartRateNotifyListener mHealthListener) {
        if(!isConnected){
            conectar_dispositivo(ctx);
        }
        new Handler().postDelayed(new Runnable() {
            public void run() {
                miBand.setHeartRateScanListener(mHealthListener);
                handler.postDelayed(myRunnable,1000);
            }
        }, 1000);

    }

    //Metodo de parar la pulsera
    public void stopCalcularPulsaciones(){
        handler.removeCallbacks(myRunnable);
    }

}
