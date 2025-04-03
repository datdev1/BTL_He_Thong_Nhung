package com.b21dccn216.vaxrobot.DevicePicking;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.b21dccn216.vaxrobot.BluetoothPresenter;
import com.b21dccn216.vaxrobot.R;
import com.b21dccn216.vaxrobot.databinding.ActivityPickDeviceBinding;

import java.util.ArrayList;
import java.util.Set;

public class PickDeviceActivity extends AppCompatActivity {
    public static final String EXTRA_DEVICE_LIST = "pairedDevices";
    private ActivityPickDeviceBinding binding;


    ArrayList<String> deviceList = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private Set<BluetoothDevice> pairedDevices;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPickDeviceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Intent intent = getIntent();
        pairedDevices = intent.getParcelableExtra(EXTRA_DEVICE_LIST);

        if (pairedDevices != null && !pairedDevices.isEmpty()) {
            for (BluetoothDevice device : pairedDevices) {
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    continue;
                }
                deviceList.add(device.getName() + " - " + device.getAddress());
            }
            adapter.notifyDataSetChanged();
        } else {
            // Tạo alert thông báo không có thiết bị nào đã kết nối
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("No Paired Devices Found")
                    .setMessage("Please pair a device and try again.")
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            // Set kết quả là fail và kết thúc activity
                            Intent resultIntent = new Intent();
                            setResult(Activity.RESULT_CANCELED, resultIntent);
                            finish();
                        }
                    })
                    .create();
            dialog.show();

        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceList);
        binding.listView.setAdapter(adapter);
    }
}