@echo off
REM Tank Battle build script: compile all .java under src and package as TankBattle.jar
setlocal
cd /d "%~dp0"

if not exist out mkdir out
echo [1/3] Compiling sources...
javac -encoding UTF-8 -d out src\*.java
if errorlevel 1 (
    echo Compile failed.
    exit /b 1
)

echo [2/3] Packaging jar...
jar cfm TankBattle.jar Manifest.txt -C out .
if errorlevel 1 (
    echo Package failed.
    exit /b 1
)

echo [3/3] Done. Run with:  java -jar TankBattle.jar
endlocal
