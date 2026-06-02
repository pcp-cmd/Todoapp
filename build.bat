@echo off
REM ============================================
REM CoolCat TodoApp - Build Script
REM ============================================
REM Before running, set the following variables
REM to match your local environment:

set JAVA_HOME=%JAVA_HOME%
set ANDROID_SDK_ROOT=%~dp0android-sdk
set ANDROID_HOME=%ANDROID_SDK_ROOT%

cd /d "%~dp0"
echo Building CoolCat APK...
echo Current directory: %cd%
echo Java: %JAVA_HOME%
dir gradlew.bat
java -version 2>&1
"%~dp0gradlew.bat" assembleDebug --no-daemon
