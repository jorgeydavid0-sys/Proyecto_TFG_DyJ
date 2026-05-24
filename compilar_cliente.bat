@echo off
echo Compilando cliente...
cd client
javac -cp ".;../lib/*" -encoding UTF-8 PlayerData.java LoginPanel.java DataDialog.java Client.java GameWindow.java GamePanel.java
if %errorlevel% == 0 (
    echo Compilacion exitosa.
) else (
    echo Error en la compilacion.
)
pause
