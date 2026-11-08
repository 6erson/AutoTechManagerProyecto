@echo off
cd /d "%~dp0"
java -jar dist\AutoTechManager.jar
if errorlevel 1 pause
