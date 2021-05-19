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

    protected MiBand miBand;
    protected BluetoothDevice device;

    public Pulsera(MiBand miBand, BluetoothDevice device) {
        this.miBand = miBand;
        this.device = device;

    }

    public void conectar_dispositivo(Context ctx) {
        final ProgressDialog pd = ProgressDialog.show(ctx, "", "Conectando al dispositivo ...");
        miBand.connect(device, new ActionCallback() {

            @Override
            public void onSuccess(Object data) {
                pd.dismiss();
                UserInfo userInfo = new UserInfo(20271234, 1, 32, 180, 80, "Usuario", 0);
                miBand.setUserInfo(userInfo);


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

    public void conectar_y_calcularPulsaciones(Context ctx, HeartRateNotifyListener mHealthListener) {
        conectar_dispositivo(ctx);
        new Handler().postDelayed(new Runnable() {
            public void run() {
                miBand.setHeartRateScanListener(mHealthListener);
                calcular_pulsaciones();
            }
        }, 1000);
    }


    private void calcular_pulsaciones() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                miBand.startHeartRateScan();
                handler.postDelayed(this, 14000);
            }
        }, 14000);
    }

}
