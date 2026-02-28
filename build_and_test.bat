@echo off
echo ========================================
echo 安卓应用时间限制 App - 构建和测试脚本
echo ========================================
echo.

set ADB_PATH=C:\Program Files\YXArkNights-12.0\shell\adb.exe

echo [1/5] 检查 ADB 连接...
"%ADB_PATH%" devices
if errorlevel 1 (
    echo 错误: 无法连接到 ADB
    echo 请确保模拟器正在运行
    pause
    exit /b 1
)
echo.

echo [2/5] 检查 Gradle...
where gradle >nul 2>&1
if errorlevel 1 (
    echo Gradle 未安装。
    echo.
    echo 请按照以下步骤手动构建:
    echo 1. 安装 Android Studio
    echo 2. 用 Android Studio 打开此项目
    echo 3. 等待 Gradle 同步完成
    echo 4. 点击 Build -^> Build Bundle^(s^) / APK^(s^) -^> Build APK^(s^)
    echo 5. APK 将生成在: app\build\outputs\apk\debug\app-debug.apk
    echo 6. 然后运行: "%ADB_PATH%" install app\build\outputs\apk\debug\app-debug.apk
    echo.
    pause
    exit /b 1
)
echo Gradle 已安装
echo.

echo [3/5] 清理旧的构建文件...
if exist app\build rmdir /s /q app\build
if exist build rmdir /s /q build
echo.

echo [4/5] 构建 APK...
call gradlew assembleDebug
if errorlevel 1 (
    echo 构建失败！
    pause
    exit /b 1
)
echo.

echo [5/5] 安装到模拟器...
"%ADB_PATH%" install -r app\build\outputs\apk\debug\app-debug.apk
if errorlevel 1 (
    echo 安装失败！
    pause
    exit /b 1
)
echo.

echo ========================================
echo 构建和安装成功！
echo ========================================
echo.
echo 应用已安装到模拟器
echo 包名: com.apptimer
echo.
echo 测试步骤:
echo 1. 在模拟器中打开"应用时间管理"应用
echo 2. 授予"使用统计"权限
echo 3. 授予"悬浮窗"权限
echo 4. 在应用列表中设置时间限制
echo 5. 打开"开始监控"开关
echo 6. 打开被限制的应用进行测试
echo.
pause
