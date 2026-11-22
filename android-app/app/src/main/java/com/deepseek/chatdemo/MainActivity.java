package com.deepseek.chatdemo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * 主界面：启停内嵌本地服务器、修改端口，并在应用内 WebView 打开前端页面。
 * API 端点 / Key / 模型等参数在网页的「设置」里调节。
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_STORAGE = 1001;

    private LocalWebServer server;

    private TextView statusText;
    private TextView addrText;
    private TextInputEditText portInput;
    private MaterialButton toggleBtn;
    private MaterialButton openBtn;
    private MaterialButton terminalBtn;
    private MaterialButton permissionBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        addrText = findViewById(R.id.addrText);
        portInput = findViewById(R.id.portInput);
        toggleBtn = findViewById(R.id.toggleBtn);
        openBtn = findViewById(R.id.openBtn);
        terminalBtn = findViewById(R.id.terminalBtn);
        permissionBtn = findViewById(R.id.permissionBtn);

        portInput.setText(String.valueOf(Prefs.getPort(this)));
        requestStoragePermissions();

        toggleBtn.setOnClickListener(v -> {
            if (isRunning()) {
                stopServer();
            } else {
                startServer();
            }
        });

        openBtn.setOnClickListener(v -> {
            if (!isRunning()) {
                Toast.makeText(this, R.string.server_not_running, Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, WebViewActivity.class);
            intent.putExtra("port", server.getListeningPort());
            startActivity(intent);
        });

        terminalBtn.setOnClickListener(v -> startActivity(new Intent(this, TerminalActivity.class)));

        permissionBtn.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            } catch (Exception e) {
                Toast.makeText(this, R.string.server_start_failed + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        refresh();
    }

    /** 存储/媒体权限在初始化时申请；无障碍与屏幕访问为可选，由系统设置里决定。 */
    private void requestStoragePermissions() {
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        List<String> missing = new ArrayList<>();
        for (String permission : permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                missing.add(permission);
            }
        }
        if (!missing.isEmpty()) {
            requestPermissions(missing.toArray(new String[0]), REQUEST_STORAGE);
        }
    }

    private boolean isRunning() {
        return server != null && server.isAlive();
    }

    private void startServer() {
        int port = readPort();
        if (port < 0) {
            Toast.makeText(this, R.string.port_invalid, Toast.LENGTH_LONG).show();
            return;
        }
        Prefs.setPort(this, port);
        try {
            server = new LocalWebServer(this, port);
            server.start();
            refresh();
            Toast.makeText(this, R.string.server_started, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            server = null;
            Toast.makeText(this, getString(R.string.server_start_failed) + e.getMessage(), Toast.LENGTH_LONG).show();
            refresh();
        }
    }

    private void stopServer() {
        if (server != null) {
            server.stop();
            server = null;
        }
        Toast.makeText(this, R.string.server_stopped, Toast.LENGTH_SHORT).show();
        refresh();
    }

    private int readPort() {
        String raw = portInput.getText() == null ? "" : portInput.getText().toString().trim();
        try {
            int port = Integer.parseInt(raw);
            if (port < 1024 || port > 65535) {
                return -1;
            }
            return port;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void refresh() {
        boolean running = isRunning();
        int port = running ? server.getListeningPort() : readPort();
        if (port < 0) {
            port = Prefs.getPort(this);
        }
        statusText.setText(running ? R.string.status_running : R.string.status_stopped);
        addrText.setText("http://127.0.0.1:" + port);
        toggleBtn.setText(running ? R.string.stop : R.string.start);
        openBtn.setEnabled(running);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (server != null) {
            server.stop();
            server = null;
        }
    }
}
