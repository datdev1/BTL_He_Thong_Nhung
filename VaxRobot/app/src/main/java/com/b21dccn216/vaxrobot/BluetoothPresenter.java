package com.b21dccn216.vaxrobot;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.b21dccn216.vaxrobot.Model.BluetoothModel;

import java.util.Set;

public class BluetoothPresenter {
    private MainActivity view;
    private BluetoothModel model;

    private Handler handler;
    private String commandSend = "S";

    public BluetoothPresenter(MainActivity view, Context context) {
        this.view = view;
        this.model = new BluetoothModel(context);
        handler = new Handler(Looper.getMainLooper());
        loopHandler();

    }

    private void loopHandler(){
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendCommand(commandSend);
                loopHandler();
            }
        }, 5);
    }

    public void connectToDevice(String deviceAddress, String name) {
        model.connectToDevice(deviceAddress,
                new BluetoothModel.ConnectionCallBack() {
                    @Override
                    public void onSuccess() {
                        view.showConnectionSuccess(name);
                    }

                    @Override
                    public void onFailure(String message) {
                        view.showConnectionFailed();
                    }
                },
                new BluetoothModel.MessageCallBack() {
                    @Override
                    public void onMessageReceived(String message) {

                        view.showMessage(message);
//                        if(message != null){
//                            String[] messages = message.split(":");
//                            if(messages[0].equals("Speed")){
//                                view.showMessage(message);
//                            }
//                        }
//                        Log.e("DATDEV1", message);
                    }
                    @Override
                    public void onError(String message) {
                        view.showError(message);
                    }
                });
    }

    public Set<BluetoothDevice> getPairedDevice(){return model.getPairedDevices();}

    public void sendCommand(String command) {
        command = command + "\n";
        model.sendData(command);
    }

    public void setCommandSend(String commandSend) {
        this.commandSend = commandSend;
        sendCommand(commandSend);
    }

    public void disconnect() {
        model.disconnect();
        view.showDisconnected();
    }




}
