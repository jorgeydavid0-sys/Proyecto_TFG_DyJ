@echo off
echo Compilando servidor...
cd server
javac -cp ".;../lib/*" -encoding UTF-8 DBManager.java GameState.java ClientHandler.java Server.java
if %errorlevel% == 0 (
    echo Compilacion exitosa.
) else (
    echo Error en la compilacion.
)
pause
