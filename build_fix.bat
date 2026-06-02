@echo off
REM ============================================
REM CoolCat TodoApp - Quick Build Script
REM ============================================
set JAVA_HOME=%JAVA_HOME%
set PATH=%JAVA_HOME%\bin;%PATH%
cd /d "%~dp0"
gradlew.bat assembleDebug
