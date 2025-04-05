package com.b21dccn216.vaxrobot.DevicePicking;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
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
import java.util.stream.Collectors;

public class PickDeviceActivity extends AppCompatActivity {
    public static final String EXTRA_DEVICE_LIST = "pairedDevices";
    private ActivityPickDeviceBinding binding;

    private ArrayList<String> deviceList = new ArrayList<>();

    private ArrayAdapter<String> adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPickDeviceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // Lấy danh sách thiết bị đã kết nối
        Intent intent = getIntent();
        deviceList = (ArrayList<String>) intent.getSerializableExtra(EXTRA_DEVICE_LIST);

        if (deviceList == null || deviceList.isEmpty()) {
            // Tạo alert thông báo không có thiết bị nào đã kết nối
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("No Paired Devices Found")
                    .setMessage("Please pair a device and try again.")
                    .setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Set kết quả là fail và kết thúc activity
                            Intent resultIntent = new Intent();
                            setResult(Activity.RESULT_CANCELED, resultIntent);
                            finish();
                        }
                    })
                    .create();
            dialog.show();
        }
        // Tạo adapter hiển thị danh sách thiết bị
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceList);
        binding.listView.setAdapter(adapter);

        // ListView item click
        binding.listView.setOnItemClickListener((adapterView, view, i, l) -> {

            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_DEVICE_LIST + "index", i);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        });

        binding.cancel.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            setResult(Activity.RESULT_CANCELED, resultIntent);
            finish();
        });
    }
}