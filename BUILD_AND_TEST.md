# 构建和测试指南

## 方法一：使用提供的批处理脚本（推荐）

### 1. 完整构建和测试
运行 `build_and_test.bat`，这个脚本会：
- 检查 ADB 连接
- 检查 Gradle 环境
- 清理旧的构建文件
- 构建 Debug APK
- 安装到模拟器

### 2. 快速安装（如果已有 APK）
运行 `install_and_test.bat`，这个脚本会：
- 卸载旧版本
- 安装新版本
- 启动应用

## 方法二：手动命令行操作

### 1. 启动模拟器
确保您的模拟器（夜神模拟器）正在运行

### 2. 检查 ADB 连接
```bash
"C:\Program Files\YXArkNights-12.0\shell\adb.exe" devices
```

应该看到类似输出：
```
List of devices attached
127.0.0.1:62001    device
```

### 3. 构建 APK
```bash
gradlew.bat assembleDebug
```

构建成功后，APK 位于：
```
app\build\outputs\apk\debug\app-debug.apk
```

### 4. 安装到模拟器
```bash
"C:\Program Files\YXArkNights-12.0\shell\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
```

### 5. 启动应用
```bash
"C:\Program Files\YXArkNights-12.0\shell\adb.exe" shell am start -n com.apptimer/.MainActivity
```

## 方法三：使用 Android Studio（最简单）

1. 安装 Android Studio
2. 打开此项目
3. 等待 Gradle 同步完成
4. 点击工具栏的绿色运行按钮
5. 选择您的模拟器设备

## 测试步骤

### 1. 首次启动 - 权限授予

1. 在模拟器中打开"应用时间管理"应用
2. 会看到黄色的权限警告提示
3. 点击警告提示，会跳转到"使用统计权限"设置页面
4. 找到"应用时间管理"，开启权限
5. 返回应用，再次点击警告（如果还有），跳转到"悬浮窗权限"设置页面
6. 开启悬浮窗权限
7. 返回应用，权限警告应该消失

### 2. 设置应用限制

1. 在应用列表中找到一个测试应用（建议选择浏览器或其他常用应用）
2. 在右侧的输入框中输入时间限制（单位：分钟）
   - 建议测试时设置为 1 分钟，方便快速验证
3. 设置会自动保存

### 3. 启动监控

1. 打开顶部的"开始监控"开关
2. 应该会在通知栏看到"应用监控中"的持续通知

### 4. 测试悬浮窗

1. 打开刚才设置了限制的应用
2. 应该在屏幕右上角看到一个圆形进度悬浮窗
3. 悬浮窗颜色会根据剩余时间变化：
   - 🟢 绿色：剩余时间充足（>50%）
   - 🟡 黄色：剩余时间警告（20%-50%）
   - 🔴 红色：剩余时间即将耗尽（<20%）
4. 可以拖动悬浮窗到任意位置

### 5. 测试超时弹窗

1. 等待设置的时间到期（如果设置了 1 分钟，就等待 1 分钟）
2. 应该会弹出全屏的"时间已到"提醒
3. 可以选择：
   - "知道了"：返回桌面
   - "再给5分钟"：延长 5 分钟使用时间

## 常见问题

### Q1: 构建失败，提示找不到 Android SDK
**A:** 需要安装 Android Studio，它会自动安装 Android SDK。或者手动下载 Android SDK 并配置环境变量。

### Q2: ADB 连接不上模拟器
**A:**
- 确保模拟器正在运行
- 尝试重启 ADB：`adb kill-server && adb start-server`
- 检查模拟器的 ADB 端口（通常是 62001）

### Q3: 应用安装后无法打开
**A:**
- 检查模拟器的 Android 版本（需要 Android 8.0 或更高）
- 查看 logcat 日志：`adb logcat | grep apptimer`

### Q4: 悬浮窗不显示
**A:**
- 确保已授予悬浮窗权限
- 某些 ROM 可能需要额外设置，在系统设置中搜索"悬浮窗"

### Q5: 监控服务被杀死
**A:**
- 将应用加入电池优化白名单
- 在系统设置中允许应用后台运行

## 调试命令

### 查看应用日志
```bash
"C:\Program Files\YXArkNights-12.0\shell\adb.exe" logcat | grep -i apptimer
```

### 查看应用信息
```bash
"C:\Program Files\YXArkNights-12.0\shell\adb.exe" shell dumpsys package com.apptimer
```

### 清除应用数据
```bash
"C:\Program Files\YXArkNights-12.0\shell\adb.exe" shell pm clear com.apptimer
```

### 卸载应用
```bash
"C:\Program Files\YXArkNights-12.0\shell\adb.exe" uninstall com.apptimer
```

## 性能监控

### 查看内存使用
```bash
"C:\Program Files\YXArkNights-12.0\shell\adb.exe" shell dumpsys meminfo com.apptimer
```

### 查看 CPU 使用
```bash
"C:\Program Files\YXArkNights-12.0\shell\adb.exe" shell top | grep apptimer
```

## 下一步

构建成功后，您可以：
1. 测试所有功能是否正常工作
2. 根据需要调整代码
3. 添加更多功能（参考 README.md 中的改进建议）
4. 构建 Release 版本用于发布
