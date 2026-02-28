# 🚀 快速开始指南

## 项目已完成！

所有代码已经实现完毕，包括 17 个 Kotlin 文件、10 个 XML 布局文件和完整的 Gradle 配置。

## 最简单的使用方法

### 步骤 1: 安装 Android Studio

下载地址：https://developer.android.com/studio

安装后会自动配置 Android SDK。

### 步骤 2: 打开项目

1. 启动 Android Studio
2. 选择 **File → Open**
3. 浏览到项目目录：`F:\AI tool\单次打开app限时项目`
4. 点击 **OK**

### 步骤 3: 等待同步

Android Studio 会自动：
- 下载必要的依赖
- 配置 Android SDK
- 同步 Gradle

这可能需要几分钟时间。

### 步骤 4: 启动模拟器

1. 打开您的夜神模拟器
2. 或者在 Android Studio 中创建新的模拟器：
   - Tools → Device Manager → Create Device

### 步骤 5: 运行应用

1. 点击工具栏的绿色 ▶️ 运行按钮
2. 选择您的模拟器设备
3. 等待构建和安装完成

### 步骤 6: 测试功能

1. **授予权限**
   - 点击黄色警告提示
   - 授予"使用统计"权限
   - 授予"悬浮窗"权限

2. **设置限制**
   - 选择一个应用（如浏览器）
   - 输入时间限制：1（分钟）

3. **开始监控**
   - 打开"开始监控"开关

4. **验证功能**
   - 打开被限制的应用
   - 观察右上角的圆形进度悬浮窗
   - 等待 1 分钟，查看超时弹窗

## 如果不想使用 Android Studio

### 使用命令行构建

**前提条件：**
- 安装 Android SDK
- 配置 local.properties 文件

**构建命令：**
```bash
gradlew.bat assembleDebug
```

**安装到模拟器：**
```bash
"C:\Program Files\YXArkNights-12.0\shell\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
```

详细说明请查看：
- `BUILD_AND_TEST.md` - 构建和测试指南
- `SDK_SETUP.md` - SDK 配置说明

## 项目文件说明

```
项目根目录/
├── app/                          # 应用模块
│   ├── src/main/
│   │   ├── java/com/apptimer/   # Kotlin 源代码
│   │   ├── res/                  # 资源文件
│   │   └── AndroidManifest.xml   # 应用清单
│   └── build.gradle.kts          # 应用构建配置
├── gradle/                       # Gradle Wrapper
├── build.gradle.kts              # 项目构建配置
├── settings.gradle.kts           # 项目设置
├── gradle.properties             # Gradle 属性
├── gradlew.bat                   # Gradle Wrapper (Windows)
├── build_and_test.bat            # 构建测试脚本
├── install_and_test.bat          # 快速安装脚本
├── README.md                     # 项目说明
├── BUILD_AND_TEST.md             # 构建指南
├── SDK_SETUP.md                  # SDK 配置
├── PROJECT_SUMMARY.md            # 项目总结
└── QUICKSTART.md                 # 本文档
```

## 功能特性

✅ 监控应用使用时间
✅ 可视化圆形进度悬浮窗
✅ 颜色渐变提示（绿→黄→红）
✅ 超时全屏弹窗提醒
✅ 延长时间功能（+5分钟）
✅ 数据持久化存储
✅ 开机自启动
✅ 前台服务持续运行

## 技术栈

- Kotlin
- Android SDK
- Room 数据库
- Kotlin 协程
- Material Design
- UsageStatsManager
- WindowManager

## 需要帮助？

查看以下文档：
- `README.md` - 完整的项目说明和功能介绍
- `BUILD_AND_TEST.md` - 详细的构建和测试步骤
- `SDK_SETUP.md` - Android SDK 配置问题解决
- `PROJECT_SUMMARY.md` - 项目实现总结

## 常见问题

**Q: 构建失败怎么办？**
A: 确保安装了 Android Studio 和 Android SDK，查看 SDK_SETUP.md

**Q: 悬浮窗不显示？**
A: 检查是否授予了悬浮窗权限，某些 ROM 需要额外设置

**Q: 应用被杀死？**
A: 将应用加入电池优化白名单

**Q: 如何查看日志？**
A: 使用命令：`adb logcat | grep apptimer`

## 下一步

1. ✅ 代码已完成
2. ⏳ 安装 Android Studio
3. ⏳ 构建项目
4. ⏳ 测试功能
5. 🎉 开始使用！

祝您使用愉快！
