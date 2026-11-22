package com.example.m3app;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

/**
 * 可选无障碍服务占位：是否启用由用户在系统「无障碍」设置里决定，
 * 开启后可读取界面内容；本应用不做强制授权。
 */
public class AccessibilityStubService extends AccessibilityService {

  @Override
  public void onAccessibilityEvent(AccessibilityEvent event) {
    // 占位服务：不主动处理事件
  }

  @Override
  public void onInterrupt() {
    // 无中断资源需要释放
  }
}
