package com.b21dccn216.vaxrobot.Main;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import com.b21dccn216.vaxrobot.Model.BluetoothModel;

import java.util.Set;

public class MainPresenter implements MainContract.Presenter {
    private MainActivity view;
    private BluetoothModel model;

    private Handler handler;

    private String commandSend = "S";
    /*  commandSend value meaning:
                                    F: Forward
                                    S: Stop
                                    B: Backward
                                    L: Left Rotate
                                    R: Right Rotate
                                    D: Delete mesage
         */


    public MainPresenter(MainActivity view, Context context) {
        this.view = view;
        init();
        handler = new Handler(Looper.getMainLooper());
        loopHandler();

    }

    @Override
    public void init(){
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Log.e("BluetoothVax", "Device does not support Bluetooth");
        if (bluetoothAdapter == null) {
            Log.e("Bluetooth", "Device does not support Bluetooth");
            view.showAlertDialog("Error", "Device does not support Bluetooth");
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Intent requestBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (ActivityCompat.checkSelfPermission((Context) view, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                view.startActivityForResult(requestBT, 1233);
            }
        }
        this.model = new BluetoothModel(view, bluetoothAdapter);
    }

    private void loopHandler(){
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendCommand(commandSend);
                loopHandler();

                switch (commandSend){
                    case "F":
//                        TODO: Thay đổi distance
                        view.updateRobotPosition("UP", 100);
                        break;
                    case "B":
                        view.updateRobotPosition("DOWN", 10);
                        break;
                    case "L":
//                        view.setRobotAngle(-15);
                        break;
                    case "R":
//                        view.setRobotAngle(15);
                        break;
                    default:
                        break;
                }
            }
        }, 5);
    }

    @Override
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

    @Override
    public Set<BluetoothDevice> getPairedDevice(){
        return model.getPairedDevices();
    }

    @Override
    public void sendCommand(String command) {
        command = command + "\n";
        model.sendData(command);
    }

    @Override
    public void setCommandSend(String commandSend) {
        this.commandSend = commandSend;
        sendCommand(commandSend);
    }

    @Override
    public void disconnect() {
        model.disconnect();
        view.showDisconnected();
    }




}
