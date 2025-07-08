@echo off
echo Compilando Test.java...
javac main/Test.java

if %errorlevel% neq 0 (
    echo Error en la compilacion
    pause
    exit /b 1
)

echo Compilacion exitosa. Ejecutando programa...
echo.

echo 0 | java main/Test

echo.
echo Programa terminado.
pause
