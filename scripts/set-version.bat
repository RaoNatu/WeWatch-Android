@echo off
setlocal enabledelayedexpansion

REM Usage: set-version.bat 1.1.0

set "VERSION=%~1"
if "%VERSION%"=="" (
    echo Usage: set-version.bat 1.1.0
    exit /b 1
)

for /f "tokens=1,2,3 delims=." %%a in ("%VERSION%") do (
    set /a MAJOR=%%a
    set /a MINOR=%%b
    set /a PATCH=%%c
)

set /a CODE=MAJOR * 10000 + MINOR * 100 + PATCH

if %CODE% LEQ 0 (
    echo Version code must be greater than 0.
    exit /b 1
)

set "GRADLE_PROPS=%~dp0..\gradle.properties"

REM Create a temporary file with updated properties
set "TMPFILE=%GRADLE_PROPS%.tmp"
(
    for /f "usebackq tokens=1,* delims==" %%i in ("%GRADLE_PROPS%") do (
        if "%%i"=="WEWATCH_VERSION_NAME" (
            echo WEWATCH_VERSION_NAME=%VERSION%
        ) else if "%%i"=="WEWATCH_VERSION_CODE" (
            echo WEWATCH_VERSION_CODE=%CODE%
        ) else (
            echo %%i=%%j
        )
    )
) > "%TMPFILE%"

move /y "%TMPFILE%" "%GRADLE_PROPS%" >nul

echo WeWatch Android version set to %VERSION% (versionCode %CODE%).
