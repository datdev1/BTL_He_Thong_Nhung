package com.b21dccn216.vaxrobot;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.b21dccn216.vaxrobot.DevicePicking.PickDeviceActivity;
import com.b21dccn216.vaxrobot.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.stream.Collectors;


public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private ActivityResultLauncher<Intent> devicePickerLauncher;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;


    private BluetoothPresenter presenter;

    private ArrayList<Pair<String, String>> deviceList = new ArrayList<>();
    private Pair<Float, Float> robotPosition = new Pair<>(5000f, 5000f);


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Inflate using Data Binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // Khởi tạo presenter
        presenter = new BluetoothPresenter(this, this);

        // Request permissions
        requestBluetoothPermissions();

        deviceList = getPairedDevices();

        devicePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        assert data != null;
                        int index = data.getIntExtra(PickDeviceActivity.EXTRA_DEVICE_LIST + "index", 0);
                        Pair<String, String> selected = deviceList.get(index);

                        presenter.connectToDevice(selected.first, selected.second);
                        Toast.makeText(this, "Name: " + selected.second + " Address: " + selected.first, Toast.LENGTH_SHORT).show();
                        // Do something with the selected device
                    }
                }
        );

        setUpButton();
    }

    private ArrayList<Pair<String, String>> getPairedDevices() {
        ArrayList<Pair<String, String>> res = new ArrayList<>();
        ArrayList<BluetoothDevice> deviceList = new ArrayList<>(presenter.getPairedDevice());
        for (BluetoothDevice device : deviceList) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                continue;
            }
            res.add(new Pair<>(device.getAddress(), device.getName()));
        }
        return res;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.disconnect();
    }

    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
            if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

                if (shouldShowRequestPermissionRationale(android.Manifest.permission.BLUETOOTH_CONNECT)) {
                    Toast.makeText(this, "Bluetooth permissions are needed to connect to devices.", Toast.LENGTH_LONG).show();
                }
                requestPermissions(new String[]{
                        android.Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                }, REQUEST_BLUETOOTH_PERMISSIONS);
            }
        }else{
            // dialog not support
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Bluetooth Permissions Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Bluetooth Permissions Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void showConnectionSuccess(String device) {
        binding.status.setImageResource(R.drawable.baseline_bluetooth_connected_24);
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
    }

    public void showConnectionFailed() {
        binding.status.setImageResource(R.drawable.baseline_do_not_disturb_24);
        Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
    }

    public void showDisconnected(){
        binding.status.setImageResource(R.drawable.baseline_do_not_disturb_24);
        Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
    }

    public void showMessage(String message){
        binding.messages.setText(message);

    }

    public void showError(String error){
        binding.messages.setText("Error: " + error);
    }


    @SuppressLint("ClickableViewAccessibility")
    private void setUpButton(){

        binding.setting.setOnClickListener(v -> {
            Intent intent = new Intent(this, PickDeviceActivity.class);
            ArrayList<String> deviceNameList = deviceList.stream()
                    .map(pair -> pair.second)
                    .collect(Collectors.toCollection(ArrayList::new));

            intent.putExtra(PickDeviceActivity.EXTRA_DEVICE_LIST, deviceNameList);
            devicePickerLauncher.launch(intent);

        });
        // Control buttons

        binding.up.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Finger touched the button
                        presenter.setCommandSend("F");
                        return true;
                    case MotionEvent.ACTION_UP:
                        // Finger lifted off the button
                        presenter.setCommandSend("S");
                        return true;
                }
                robotPosition = new Pair<>(robotPosition.first, robotPosition.second - 10);
                binding.mapView.updateRobotPosition(robotPosition.first, robotPosition.second);
                return false;
            }
        });
        binding.down.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Finger touched the button
                        presenter.setCommandSend("B");
                        return true;
                    case MotionEvent.ACTION_UP:
                        // Finger lifted off the button
                        presenter.setCommandSend("S");
                        return true;
                }
                robotPosition = new Pair<>(robotPosition.first, robotPosition.second + 10);
                binding.mapView.updateRobotPosition(robotPosition.first, robotPosition.second);
                return false;
            }
        });

        binding.left.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Finger touched the button
                        presenter.setCommandSend("L");
                        return true;
                    case MotionEvent.ACTION_UP:
                        // Finger lifted off the button
                        presenter.setCommandSend("S");
                        return true;
                }
                robotPosition = new Pair<>(robotPosition.first - 10, robotPosition.second);
                binding.mapView.updateRobotPosition(robotPosition.first, robotPosition.second);
                return false;
            }
        });

        binding.right.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Finger touched the button
                        presenter.setCommandSend("R");
                        return true;
                    case MotionEvent.ACTION_UP:
                        // Finger lifted off the button
                        presenter.setCommandSend("S");
                        return true;
                }
                robotPosition = new Pair<>(robotPosition.first + 10, robotPosition.second);
                binding.mapView.updateRobotPosition(robotPosition.first, robotPosition.second);
                return false;
            }
        });

//        binding.seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
//
//
//                String res = "Speed " + String.valueOf(i);
//                presenter.sendCommand(res);
//
//                binding.seekbarValue.setText(i + "");
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {
//
//            }
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {
//
//            }
//        });
//
//        binding.seekbarDelta.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
//
//
//                String res = "Delta_speed " + String.valueOf(i-50);
//                presenter.sendCommand(res);
//
//                binding.seekbarDeltaValue.setText("∆ = " +  String.valueOf(i-50));
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {
//
//            }
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {
//
//            }
//        });

        binding.delete.setOnClickListener(v -> {
            presenter.sendCommand("D");
        });
    }
}