@echo off
echo ========================================
echo 快速安装和测试脚本
echo ========================================
echo.

set ADB_PATH=C:\Program Files\YXArkNights-12.0\shell\adb.exe
set APK_PATH=app\build\outputs\apk\debug\app-debug.apk

echo 检查 APK 文件...
if not exist "%APK_PATH%" (
    echo 错误: 找不到 APK 文件
    echo 路径: %APK_PATH%
    echo.
    echo 请先构建项目！
    pause
    exit /b 1
)
echo.

echo 检查 ADB 连接...
"%ADB_PATH%" devices
echo.

echo 卸载旧版本...
"%ADB_PATH%" uninstall com.apptimer 2>nul
echo.

echo 安装新版本...
"%ADB_PATH%" install "%APK_PATH%"
if errorlevel 1 (
    echo 安装失败！
    pause
    exit /b 1
)
echo.

echo ========================================
echo 安装成功！
echo ========================================
echo.
echo 启动应用...
"%ADB_PATH%" shell am start -n com.apptimer/.MainActivity
echo.
echo 应用已启动，请在模拟器中查看
pause
