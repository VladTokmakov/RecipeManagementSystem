@echo off
rem Stop RecipeManagementSystem.
rem See start.cmd - PowerShell policy is bypassed for this call only.
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop.ps1" %*
exit /b %ERRORLEVEL%
