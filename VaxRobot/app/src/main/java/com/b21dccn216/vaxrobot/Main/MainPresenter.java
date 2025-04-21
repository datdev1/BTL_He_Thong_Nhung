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

import androidx.core.app.ActivityCompat;

import com.b21dccn216.vaxrobot.Model.BluetoothModel;

import java.util.Set;

public class MainPresenter implements MainContract.Presenter {

    private MainActivity view;
    private BluetoothModel model;

    private Handler handler;


    private boolean isShowSeekBaGroup = true;

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
                        view.updateRobotPosition("UP", 10);
                        break;
                    case "B":
                        view.updateRobotPosition("DOWN", 10);
                        break;
                    case "L":
                        view.setRobotAngle(-45);
                        break;
                    case "R":
                        view.setRobotAngle(45);
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

    void parseEspMessage(String message) {
        message = message.trim().replace("\r", "");
        String[] lines = message.split("\n");

        for (String line : lines) {
            if (line.startsWith("Speed:")) {
                String[] parts = line.split("; ");
                float speed = Float.parseFloat(parts[0].split(": ")[1]);
                float travelDis = Float.parseFloat(parts[1].split(": ")[1]);

                // Save or use values
            } else if (line.startsWith("SpeedMotor:")) {
                String[] parts = line.split("; ");
                int speedMotor = Integer.parseInt(parts[0].split(": ")[1]);
                int deltaSpeed = Integer.parseInt(parts[1].split(": ")[1]);
                float vong = Float.parseFloat(parts[2].split(": ")[1]);

                // Save or use values
            } else if (line.startsWith("Ultrasonic:")) {
                line = line.replaceAll("[^0-9;: ]", ""); // Clean brackets
                String[] parts = line.split("; ");
                int left = Integer.parseInt(parts[0].split(": ")[1]);
                int right = Integer.parseInt(parts[1].split(": ")[1]);
                int front = Integer.parseInt(parts[2].split(": ")[1]);

                // Save or use values
            } else if (line.startsWith("Accel:")) {
                line = line.replaceAll("[^0-9.-;: ]", ""); // Clean brackets
                String[] parts = line.split("; ");
                float accelX = Float.parseFloat(parts[0].split(": ")[1]);
                float accelY = Float.parseFloat(parts[1].split(": ")[1]);
                float accelZ = Float.parseFloat(parts[2].split(": ")[1]);

                // Save or use values
            } else if (line.startsWith("Gyro:")) {
                line = line.replaceAll("[^0-9.-;: ]", "");
                String[] parts = line.split("; ");
                float gyroX = Float.parseFloat(parts[0].split(": ")[1]);
                float gyroY = Float.parseFloat(parts[1].split(": ")[1]);
                float gyroZ = Float.parseFloat(parts[2].split(": ")[1]);

                // Save or use values
            } else if (line.startsWith("Compass:")) {
                line = line.replaceAll("[^0-9.-;: ]", "");
                String[] parts = line.split("; ");
                float compassX = Float.parseFloat(parts[0].split(": ")[1]);
                float compassY = Float.parseFloat(parts[1].split(": ")[1]);
                float compassZ = Float.parseFloat(parts[2].split(": ")[1]);
                float heading = Float.parseFloat(parts[3].split(": ")[1]);

                // Save or use values
            }
        }
    }

    public void setIsShowSeekBarGroup(boolean isShow){
        view.setVisibleSeekBarGroup(isShow);
    }


}
