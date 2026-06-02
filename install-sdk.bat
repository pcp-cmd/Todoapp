@echo off
REM ============================================
REM CoolCat TodoApp - SDK Install Script
REM ============================================
REM Before running, make sure JAVA_HOME is set
REM in your system environment variables.

set JAVA_HOME=%JAVA_HOME%
set PATH=%JAVA_HOME%\bin;%PATH%
set SDK_ROOT=%~dp0android-sdk

echo Installing SDK components...
echo y | "%SDK_ROOT%\cmdline-tools\latest\bin\sdkmanager.bat" --sdk_root="%SDK_ROOT%" "platform-tools" "build-tools;34.0.0" "platforms;android-34"
echo Done!
