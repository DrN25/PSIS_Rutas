@echo off
echo Compilando todos los archivos Java...
javac -cp . main/Test.java algorithms/*.java models/*.java gui/*.java loader/*.java graph/*.java utils/*.java

if %errorlevel% neq 0 (
    echo Error en la compilacion
    pause
    exit /b 1
)

echo Compilacion exitosa. Ejecutando programa...
echo.

java -cp . main.Test

echo.
echo Programa terminado.
pause
