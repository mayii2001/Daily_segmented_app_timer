@echo off
echo 下载 Gradle Wrapper...
echo.

set WRAPPER_URL=https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar
set WRAPPER_DIR=gradle\wrapper
set WRAPPER_JAR=%WRAPPER_DIR%\gradle-wrapper.jar

if not exist "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"

echo 正在下载 gradle-wrapper.jar...
powershell -Command "& {Invoke-WebRequest -Uri '%WRAPPER_URL%' -OutFile '%WRAPPER_JAR%'}"

if exist "%WRAPPER_JAR%" (
    echo.
    echo 下载成功！
    echo.
    echo 现在可以运行 gradlew.bat 来构建项目
) else (
    echo.
    echo 下载失败！
    echo.
    echo 请手动下载 gradle-wrapper.jar 并放置到 %WRAPPER_DIR% 目录
)

pause
