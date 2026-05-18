@echo off
echo Compilando servidor...
cd server
javac *.java
if %errorlevel% neq 0 ( echo ERROR al compilar servidor & pause & exit /b )
echo OK

echo Compilando cliente...
cd ..\client
javac *.java
if %errorlevel% neq 0 ( echo ERROR al compilar cliente & pause & exit /b )
echo OK

cd ..
echo.
echo Compilacion completada. Usa iniciar_servidor.bat e iniciar_cliente.bat
pause
