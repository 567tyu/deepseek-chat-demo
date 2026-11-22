package com.example.m3app;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 内置命令行：在应用沙箱工作目录下通过 /system/bin/sh 执行一条命令，
 * 回传标准输出、标准错误与退出码。本地 Web 与应用内终端共用同一执行入口。
 */
public final class ShellSession {

  public static final class Result {
    public final String stdout;
    public final String stderr;
    public final int code;

    Result(String stdout, String stderr, int code) {
      this.stdout = stdout;
      this.stderr = stderr;
      this.code = code;
    }
  }

  private ShellSession() {}

  public static Result exec(Context ctx, String command) {
    final StringBuilder out = new StringBuilder();
    final StringBuilder err = new StringBuilder();
    int code = -1;
    Process process = null;
    try {
      process = Runtime.getRuntime().exec(
          new String[]{"/system/bin/sh", "-c", command}, null, ctx.getFilesDir());

      BufferedReader stdout = new BufferedReader(
          new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
      BufferedReader stderr = new BufferedReader(
          new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));

      Thread outReader = new Thread(() -> readAll(stdout, out));
      Thread errReader = new Thread(() -> readAll(stderr, err));
      outReader.start();
      errReader.start();
      outReader.join();
      errReader.join();
      code = process.waitFor();
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      err.append("命令已中断\n");
    } catch (Exception ex) {
      String message = ex.getMessage() == null ? ex.toString() : ex.getMessage();
      err.append(message).append('\n');
    } finally {
      if (process != null) {
        process.destroy();
      }
    }
    return new Result(out.toString(), err.toString(), code);
  }

  private static void readAll(BufferedReader reader, StringBuilder sink) {
    try {
      String line;
      while ((line = reader.readLine()) != null) {
        sink.append(line).append('\n');
      }
    } catch (Exception ignored) {
      // 读取中断时保留已读到的内容
    }
  }
}
