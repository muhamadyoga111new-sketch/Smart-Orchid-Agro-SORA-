package com.example.itprojek;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class BluetoothSetupActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 101;
    private static final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // SPP UUID

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;

    private Button btnScan, btnSendConfig;
    private TextView tvStatus;
    private ListView listDevices;
    private TextInputEditText etSsid, etPassword;

    private ArrayAdapter<String> deviceListAdapter;
    private ArrayList<BluetoothDevice> deviceList;
    private ArrayList<String> deviceNameList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_setup);

        btnScan = findViewById(R.id.btn_scan_bluetooth);
        btnSendConfig = findViewById(R.id.btn_send_config);
        tvStatus = findViewById(R.id.tv_bluetooth_status);
        listDevices = findViewById(R.id.list_bluetooth_devices);
        etSsid = findViewById(R.id.et_wifi_ssid);
        etPassword = findViewById(R.id.et_wifi_password);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        
        hideSystemNavBar();
        applyStatusBarInsets();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        deviceList = new ArrayList<>();
        deviceNameList = new ArrayList<>();
        deviceListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceNameList);
        listDevices.setAdapter(deviceListAdapter);

        if (bluetoothAdapter == null) {
            tvStatus.setText("Perangkat ini tidak mendukung Bluetooth.");
            btnScan.setEnabled(false);
            return;
        }

        btnScan.setOnClickListener(v -> {
            if (checkBluetoothPermissions()) {
                scanDevices();
            }
        });

        listDevices.setOnItemClickListener((parent, view, position, id) -> {
            BluetoothDevice device = deviceList.get(position);
            connectToDevice(device);
        });

        btnSendConfig.setOnClickListener(v -> {
            String ssid = etSsid.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();
            
            if (ssid.isEmpty()) {
                Toast.makeText(this, "SSID tidak boleh kosong", Toast.LENGTH_SHORT).show();
                return;
            }
            sendWiFiConfig(ssid, pass);
        });
    }

    private boolean checkBluetoothPermissions() {
        ArrayList<String> permissionsNeeded = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }
    
    private void hideSystemNavBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            android.view.WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(android.view.WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    private void applyStatusBarInsets() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(getWindow().getDecorView(), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
            int statusH  = systemBars.top;
            int base60px = (int) (60 * getResources().getDisplayMetrics().density);
            View headerBg = findViewById(R.id.header_bg);
            if (headerBg != null) {
                headerBg.getLayoutParams().height = base60px + statusH;
                headerBg.requestLayout();
            }
            View btnBack = findViewById(R.id.btn_back);
            if (btnBack != null && btnBack.getLayoutParams() instanceof androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) {
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams p = (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) btnBack.getLayoutParams();
                p.topMargin = statusH;
                btnBack.setLayoutParams(p);
            }
            View tvTitle = findViewById(R.id.tv_header_title);
            if (tvTitle != null && tvTitle.getLayoutParams() instanceof androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) {
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams p = (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) tvTitle.getLayoutParams();
                p.topMargin = statusH;
                tvTitle.setLayoutParams(p);
            }
            return insets;
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                scanDevices();
            } else {
                Toast.makeText(this, "Izin Bluetooth ditolak. Fitur ini tidak bisa digunakan.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void scanDevices() {
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Silakan aktifkan Bluetooth terlebih dahulu.", Toast.LENGTH_SHORT).show();
            return;
        }

        deviceList.clear();
        deviceNameList.clear();
        deviceListAdapter.notifyDataSetChanged();
        listDevices.setVisibility(View.VISIBLE);
        tvStatus.setText("Mencari perangkat...");

        // Menambahkan perangkat yang sudah di-pair (Paired Devices)
        try {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (pairedDevices != null && pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    String name = device.getName();
                    if (name == null) name = "Unknown Device";
                    deviceList.add(device);
                    deviceNameList.add(name + " (Paired)\n" + device.getAddress());
                }
                deviceListAdapter.notifyDataSetChanged();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        // Mulai memindai perangkat baru
        try {
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            bluetoothAdapter.startDiscovery();
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(receiver, filter);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    try {
                        String name = device.getName();
                        if (name == null) name = "Unknown Device";
                        String addr = device.getAddress();
                        
                        // Hindari duplikat
                        boolean exists = false;
                        for (BluetoothDevice d : deviceList) {
                            if (d.getAddress().equals(addr)) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            deviceList.add(device);
                            deviceNameList.add(name + "\n" + addr);
                            deviceListAdapter.notifyDataSetChanged();
                        }
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    private void connectToDevice(BluetoothDevice device) {
        try {
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        tvStatus.setText("Menghubungkan ke " + getDeviceName(device) + "...");
        btnScan.setEnabled(false);
        listDevices.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(BT_UUID);
                bluetoothSocket.connect();
                outputStream = bluetoothSocket.getOutputStream();

                runOnUiThread(() -> {
                    tvStatus.setText("Terhubung ke " + getDeviceName(device));
                    tvStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
                    btnSendConfig.setEnabled(true);
                    btnScan.setEnabled(true);
                    
                    // 2. Dari aplikasi Android kirim perintah: SETUP
                    sendData("SETUP\n");
                    Toast.makeText(this, "Perintah SETUP terkirim, ESP32 bersiap menerima konfigurasi.", Toast.LENGTH_SHORT).show();
                });

            } catch (IOException | SecurityException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    tvStatus.setText("Gagal menghubungkan ke " + getDeviceName(device));
                    tvStatus.setTextColor(android.graphics.Color.parseColor("#F44336"));
                    btnScan.setEnabled(true);
                    listDevices.setVisibility(View.VISIBLE);
                });
                closeConnection();
            }
        }).start();
    }

    private String getDeviceName(BluetoothDevice device) {
        try {
            String name = device.getName();
            return name != null ? name : device.getAddress();
        } catch (SecurityException e) {
            return "Unknown";
        }
    }

    private void sendWiFiConfig(String ssid, String password) {
        if (outputStream == null) {
            Toast.makeText(this, "Tidak terhubung ke ESP32!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 5. Aplikasi mengirim SSID dan password baru melalui Bluetooth.
        // Format yang direkomendasikan untuk dibaca oleh ESP32:
        String configData = "SSID=" + ssid + "\nPASS=" + password + "\n";
        
        sendData(configData);
        Toast.makeText(this, "Konfigurasi terkirim! ESP32 akan restart.", Toast.LENGTH_LONG).show();
        
        // Tutup koneksi setelah mengirim
        new Handler(Looper.getMainLooper()).postDelayed(this::closeConnection, 1000);
    }

    private void sendData(String data) {
        try {
            if (outputStream != null) {
                outputStream.write(data.getBytes());
                outputStream.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal mengirim data", Toast.LENGTH_SHORT).show();
        }
    }

    private void closeConnection() {
        try {
            if (outputStream != null) outputStream.close();
            if (bluetoothSocket != null) bluetoothSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        outputStream = null;
        bluetoothSocket = null;
        
        runOnUiThread(() -> {
            tvStatus.setText("Koneksi ditutup.");
            tvStatus.setTextColor(android.graphics.Color.parseColor("#757575"));
            btnSendConfig.setEnabled(false);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeConnection();
        try {
            unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            // Receiver not registered
        }
    }
}
