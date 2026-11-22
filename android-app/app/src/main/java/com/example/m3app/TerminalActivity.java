package com.example.m3app;

import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

/**
 * 应用内命令行：在本机沙箱目录执行 shell 命令并回显输出。
 * 本地 Web 页面也可通过内嵌服务的 /api/exec 入口让 AI 执行命令。
 */
public class TerminalActivity extends AppCompatActivity {

  private TextView console;
  private ScrollView consoleScroll;
  private TextInputEditText commandInput;
  private MaterialButton runBtn;
  private MaterialButton clearBtn;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_terminal);

    console = findViewById(R.id.console);
    consoleScroll = findViewById(R.id.consoleScroll);
    commandInput = findViewById(R.id.commandInput);
    runBtn = findViewById(R.id.runBtn);
    clearBtn = findViewById(R.id.clearBtn);

    findViewById(R.id.terminalToolbar).setOnClickListener(v -> finish());

    append(getString(R.string.terminal_hint));
    append("");

    runBtn.setOnClickListener(v -> runCommand());
    clearBtn.setOnClickListener(v -> console.setText(""));

    commandInput.setOnEditorActionListener((v, actionId, event) -> {
      if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND) {
        runCommand();
        return true;
      }
      return false;
    });
  }

  private void runCommand() {
    String command = commandInput.getText() == null ? "" : commandInput.getText().toString().trim();
    if (command.isEmpty()) {
      Toast.makeText(this, R.string.terminal_command_empty, Toast.LENGTH_SHORT).show();
      return;
    }
    commandInput.setText("");
    append("$ " + command);
    runBtn.setEnabled(false);

    new Thread(() -> {
      ShellSession.Result result = ShellSession.exec(this, command);
      runOnUiThread(() -> {
        if (!result.stdout.isEmpty()) {
          append(result.stdout.stripTrailing());
        }
        if (!result.stderr.isEmpty()) {
          append(result.stderr.stripTrailing());
        }
        append(getString(R.string.terminal_exit_code, result.code));
        append("");
        runBtn.setEnabled(true);
      });
    }).start();
  }

  private void append(String text) {
    CharSequence current = console.getText();
    if (current.length() == 0) {
      console.setText(text);
    } else {
      console.append("\n" + text);
    }
    consoleScroll.post(() -> consoleScroll.fullScroll(ScrollView.FOCUS_DOWN));
  }
}
