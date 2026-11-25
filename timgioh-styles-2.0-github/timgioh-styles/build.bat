@echo off
chcp 65001 >nul
echo ============================================
echo  timgioh styles bauen (braucht Java 21 + Internet)
echo ============================================
call gradlew.bat build
if errorlevel 1 (
    echo.
    echo BAUEN FEHLGESCHLAGEN - bitte die Fehlermeldung oben kopieren.
    pause
    exit /b 1
)
echo.
echo Fertig! Die Mod liegt in: build\libs\timgioh-styles-2.0.0.jar
echo Diese Datei in den mods-Ordner legen (alte timgioh-styles-Version vorher loeschen).
pause
