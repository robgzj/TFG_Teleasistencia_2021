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
    private Handler handler=new Handler();;
    private Runnable myRunnable= new Runnable() {
        public void run() {
            miBand.startHeartRateScan();
            handler.postDelayed(this, 14000);
        }
    };
    private boolean isConnected;
    private ProgressDialog pd;

    public Pulsera(MiBand miBand, BluetoothDevice device) {
        this.miBand = miBand;
        this.device = device;
    }

    public void conectar_dispositivo(Context ctx) {
        pd = ProgressDialog.show(ctx, "", "Conectando al dispositivo ...");
        miBand.connect(device, new ActionCallback() {

            @Override
            public void onSuccess(Object data) {

                UserInfo userInfo = new UserInfo(20271234, 1, 32, 180, 80, "Usuario", 0);
                miBand.setUserInfo(userInfo);
                isConnected=true;
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

    public void stopCalcularPulsaciones(){
        handler.removeCallbacks(myRunnable);
    }

}
