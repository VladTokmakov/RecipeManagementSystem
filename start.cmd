@echo off
rem Start RecipeManagementSystem.
rem Wrapper is required because the default PowerShell policy (Restricted)
rem forbids running .ps1 files. Policy is bypassed for this call only;
rem no system settings are changed.
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start.ps1" %*
exit /b %ERRORLEVEL%
