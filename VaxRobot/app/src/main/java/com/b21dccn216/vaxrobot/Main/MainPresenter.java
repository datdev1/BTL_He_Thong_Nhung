package com.b21dccn216.vaxrobot.Main;

import static com.b21dccn216.vaxrobot.View.MapView.squareSizeCm;

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

import javax.inject.Inject;
import javax.inject.Singleton;


@Singleton
public class MainPresenter implements MainContract.Presenter {

    private MainActivity view;
    private BluetoothModel model;

    private Handler handler;

    private float travelDistance = 0;
    private float traveledDistance = 0;


    private int compassAngle = 0;
    private float yAngle = 0;

    private BluetoothAdapter bluetoothAdapter;

    private boolean isShowSeekBaGroup = true;
    private boolean isSettingCompass = false;

    private String commandSend = "S";
    /*  commandSend value meaning:
                                    F: Forward
                                    S: Stop
                                    B: Backward
                                    L: Left Rotate
                                    R: Right Rotate
                                    D: Delete mesage
         */

    @Inject
    public MainPresenter() {
    }

    public void setView(MainActivity view){
        this.view = view;
        init();
        handler = new Handler(Looper.getMainLooper());
        loopHandler();
    }

    @Override
    public void init(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Log.e("BluetoothVax", "Device does not support Bluetooth");
        if (bluetoothAdapter == null) {
            Log.e("Bluetooth", "Device does not support Bluetooth");
            view.showAlertDialog("Error", "Device does not support Bluetooth");
        } else {
            // Request to turn bluetooth on
            requestToTurnBluetoothOn();
        }
        // Init model
        this.model = new BluetoothModel(view, bluetoothAdapter);
    }

    private void requestToTurnBluetoothOn(){
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

    private void loopHandler(){
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendCommand(commandSend);
                loopHandler();
                view.updateRobotAction(commandSend);
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
                        parseBluetoothMessage(message);
                        int mapped = (int) yAngle;
                        if(mapped < 0) mapped = 360 + mapped;
//                        Log.d("MapView", "CompassAngle: " + compassAngle);
                        view.setRobotAngle(mapped);

                        float delta =  (travelDistance - traveledDistance);
                        if(delta / squareSizeCm > 1){
                            Log.d("MapView", "1delta: " + delta);
                            Log.d("MapView", "2travelDistance: " + travelDistance);

                            int intDelta = (int) (delta - delta % squareSizeCm);

                            view.updateRobotPosition(intDelta);
                            traveledDistance += (float)(intDelta);
                            Log.d("MapView", "3intDelta: " + intDelta);
                            Log.d("MapView", "4traveledDistance: " + traveledDistance);
                        }

                        // TODO:: UPDATE MAP
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

    // TODO:: get Heading.
    void parseBluetoothMessage(String fullMessage) {

        String[] lines = fullMessage.strip().split("\\R"); // split by line
//        Log.e("MapView", "lines " + lines.length + " message " + fullMessage);

        // 1. Speed and Travel Distance
        try{
            String[] speedParts = lines[0].split("; ");
            double speed = Double.parseDouble(speedParts[0].split(": ")[1]);
            travelDistance = (float) Double.parseDouble(speedParts[1].split(": ")[1]);
//            Log.i("MapView", "Speed = " + speed + ", Travel Distance = " + travelDis);
        }catch (Exception e){
            Log.e("MapView", "parseBluetoothMessage: " + e.getStackTrace().toString());
        }


        // 2. Motor Speed
//        try{
//            String[] motorParts = lines[1].split("; ");
//            int speedMotor = Integer.parseInt(motorParts[0].split(": ")[1]);
//            int deltaSpeed = Integer.parseInt(motorParts[1].split(": ")[1]);
//            double vong = Double.parseDouble(motorParts[2].split(": ")[1]);
//            Log.i("MapView", "Motor = " + speedMotor + ", Delta = " + deltaSpeed + ", Vong = " + vong);
//        }catch (Exception e){
//            Log.e("MapView", "parseBluetoothMessage: " + e.getStackTrace().toString());
//        }

        // 3. Sonic
//        String[] sonicParts = lines[2].replaceAll("[^0-9;]", "").split("; ");
//        int sonicL = Integer.parseInt(sonicParts[0]);
//        int sonicR = Integer.parseInt(sonicParts[1]);
//        int sonicF = Integer.parseInt(sonicParts[2]);

        // 4. Accelerometer
//        String[] accelParts = lines[3].replaceAll("[^X0-9Y:Z\\-; ]", "").split("[; ]+");
//        int accelX = Integer.parseInt(accelParts[1]);
//        int accelY = Integer.parseInt(accelParts[3]);
//        int accelZ = Integer.parseInt(accelParts[5]);

        // 5. Gyroscope
//        String[] gyroParts = lines[4].replaceAll("[^X0-9Y:Z\\-; ]", "").split("[; ]+");
//        int gyroX = Integer.parseInt(gyroParts[1]);
//        int gyroY = Integer.parseInt(gyroParts[3]);
//        int gyroZ = Integer.parseInt(gyroParts[5]);

        // 6. Yaw-Pitch-Roll
        try{
            String[] yprParts = lines[5]
                    .replace("YPR:", "")
                    .replace("[", "")
                    .replace("]", "")
                    .split(";");
            yAngle = Float.parseFloat(yprParts[0].replace("Y:", ""));
            double pitch = Double.parseDouble(yprParts[1].replace("P:", ""));
            double roll = Double.parseDouble(yprParts[2].replace("R:", ""));
//            Log.i("MapView", "YPR = Yaw: " + yAngle + ", Pitch: " + pitch + ", Roll: " + roll);

        }catch (Exception e){
            Log.e("MapView", "parseBluetoothMessage: " + e.getStackTrace().toString() + e.getMessage());
        }

        // 7. Temp & Pressure
//        String[] tempParts = lines[6].split("; ");
//        double temp = Double.parseDouble(tempParts[0].split(": ")[1].replace("'C", ""));
//        int pressure = Integer.parseInt(tempParts[1].split(": ")[1].replace("Pa", ""));

        // 8. Compass
        try{
            String[] compassParts = lines[7].replaceAll("[^0-9\\-; ]", "").split("; ");
            Log.d("MapView", compassParts[3]);
//            int compassX = Integer.parseInt(compassParts[0].split(": ")[1]);
//            int compassY = Integer.parseInt(compassParts[1].split(": ")[1]);
//            int compassZ = Integer.parseInt(compassParts[2].split(": ")[1]);
            compassAngle = Integer.parseInt(compassParts[3].trim());
//            Log.i("MapView", "Compass X/Y/Z = " + compassAngle + "/" + compassAngle + "/" + compassAngle + ", Heading = " + compassAngle);

        }catch (Exception e){
            Log.e("MapView", "parseBluetoothMessage: " + e.getStackTrace()+ " " + e.getMessage());
        }

        // Print sample values
        // Inside your method
//        Log.i("MapView", "Sonic L/R/F = " + sonicL + "/" + sonicR + "/" + sonicF);
//        Log.i("MapView", "Accel X/Y/Z = " + accelX + "/" + accelY + "/" + accelZ);
//        Log.i("MapView", "Gyro X/Y/Z = " + gyroX + "/" + gyroY + "/" + gyroZ);
//        Log.i("MapView", "Temp = " + temp + "°C, Pressure = " + pressure + " Pa");
    }



    public void setIsShowSeekBarGroup(boolean isShow){
        view.setVisibleSeekBarGroup(isShow);
        isShowSeekBaGroup = isShow;
    }

    public boolean isSettingCompass() {
        return isSettingCompass;
    }

    public void setSettingCompass(boolean settingCompass) {
        isSettingCompass = settingCompass;
        if(settingCompass){
            sendCommand("calculatingCalibration");
            view.toastMessage("calculatingCalibration");
            return;
        }
        sendCommand("resetCalibration");
        view.toastMessage("resetCalibration");
    }

    public boolean isShowSeekBaGroup() {
        return isShowSeekBaGroup;
    }
}
