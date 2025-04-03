package com.b21dccn216.vaxrobot;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Pair;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.b21dccn216.vaxrobot.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.Set;


public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;

    private BluetoothPresenter presenter;

    ArrayList<String> deviceList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private Pair<Float, Float> robotPosition = new Pair<>(0f, 0f);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Inflate using Data Binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // Khởi tạo presenter
        presenter = new BluetoothPresenter(this, this);
        // Adapter hiển thị danh sách thiết bị bluetooth đã kết nối
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceList);
        binding.listView.setAdapter(adapter);

        // Request permissions
        requestBluetoothPermissions();

        // Handle insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Populate paired devices
        loadPairedDevices();

        // ListView item click
        binding.listView.setOnItemClickListener((adapterView, view, i, l) -> {
            BluetoothDevice[] devicesArray = presenter.getPairedDevice().toArray(new BluetoothDevice[0]);
            if (i < devicesArray.length) {
                BluetoothDevice selectedDevice = devicesArray[i];
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                presenter.connectToDevice(selectedDevice.getAddress(), selectedDevice.getName());
            }
        });

        // Control buttons
        binding.up.setOnClickListener(v -> {
            presenter.sendCommand("F");
            robotPosition = new Pair<>(robotPosition.first, robotPosition.second - 10);
            binding.mapView.updateRobotPosition(robotPosition.first, robotPosition.second);
        });
        binding.down.setOnClickListener(v -> {
            presenter.sendCommand("B");
            robotPosition = new Pair<>(robotPosition.first, robotPosition.second + 10);
            binding.mapView.updateRobotPosition(robotPosition.first, robotPosition.second);
        });
        binding.left.setOnClickListener(v -> {
            presenter.sendCommand("L");
            robotPosition = new Pair<>(robotPosition.first - 10, robotPosition.second);
            binding.mapView.updateRobotPosition(robotPosition.first, robotPosition.second);
        });
        binding.right.setOnClickListener(v -> {
            presenter.sendCommand("R");
            robotPosition = new Pair<>(robotPosition.first + 10, robotPosition.second);
            binding.mapView.updateRobotPosition(robotPosition.first, robotPosition.second);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.disconnect();
    }

    private void loadPairedDevices() {
        Set<BluetoothDevice> pairedDevices = presenter.getPairedDevice();
        deviceList.clear();

        if (pairedDevices != null && !pairedDevices.isEmpty()) {
            for (BluetoothDevice device : pairedDevices) {
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    continue;
                }
                deviceList.add(device.getName() + " - " + device.getAddress());
            }
            adapter.notifyDataSetChanged();
        } else {
            Toast.makeText(this, "No Paired Devices Found", Toast.LENGTH_SHORT).show();
        }
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
        binding.status.setText("Connected to " + device);
        binding.status.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
    }

    public void showConnectionFailed() {
        binding.status.setText("Connection Failed");
        binding.status.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
    }

    public void showDisconnected(){
        binding.status.setText("Disconnected");
        binding.status.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
    }

    public void showMessage(String message){
        binding.messages.setText("Message: " + message);

    }

    public void showError(String error){
        binding.messages.setText("Error: " + error);
    }



}