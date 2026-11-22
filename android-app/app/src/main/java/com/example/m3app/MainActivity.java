package com.example.m3app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

/**
 * 主界面：启停内嵌本地服务器、修改端口，并在应用内 WebView 打开前端页面。
 * API 端点 / Key / 模型等参数在网页的「设置」里调节。
 */
public class MainActivity extends AppCompatActivity {

    private LocalWebServer server;

    private TextView statusText;
    private TextView addrText;
    private TextInputEditText portInput;
    private MaterialButton toggleBtn;
    private MaterialButton openBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        addrText = findViewById(R.id.addrText);
        portInput = findViewById(R.id.portInput);
        toggleBtn = findViewById(R.id.toggleBtn);
        openBtn = findViewById(R.id.openBtn);

        portInput.setText(String.valueOf(Prefs.getPort(this)));

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

        refresh();
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
