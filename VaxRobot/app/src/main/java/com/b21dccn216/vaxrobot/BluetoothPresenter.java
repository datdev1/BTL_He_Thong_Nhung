package com.b21dccn216.vaxrobot;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.b21dccn216.vaxrobot.Model.BluetoothModel;

import java.util.Set;

public class BluetoothPresenter {
    private MainActivity view;
    private BluetoothModel model;

    public BluetoothPresenter(MainActivity view, Context context) {
        this.view = view;
        this.model = new BluetoothModel(context);
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
                    }
                    @Override
                    public void onError(String message) {
                        view.showError(message);
                    }
                });
    }

    public Set<BluetoothDevice> getPairedDevice(){return model.getPairedDevices();}

    public void sendCommand(String command) {
        model.sendData(command);
    }

    public void disconnect() {
        model.disconnect();
        view.showDisconnected();
    }




}
