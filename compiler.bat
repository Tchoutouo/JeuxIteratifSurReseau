@echo off
echo Compilation des fichiers source Java...

rem Navigue dans le dossier source et compile tous les fichiers .java
javac src\jeux\*.java

if %errorlevel% equ 0 (
    echo Compilation terminee avec succes.
) else (
    echo Erreur lors de la compilation.
)
pause