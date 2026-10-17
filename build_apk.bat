@echo off
setlocal
echo ======================================================================
echo   LOF Titan S3 EOL PCB Test Fixture - APK Build Helper
echo ======================================================================
echo.

set SCRIPT_DIR=%~dp0

rem Auto-detect portable JDK 17 if installed in workspace
if exist "%SCRIPT_DIR%jdk17\jdk-17.0.10+7\bin\java.exe" (
    set "JAVA_HOME=%SCRIPT_DIR%jdk17\jdk-17.0.10+7"
    set "PATH=%SCRIPT_DIR%jdk17\jdk-17.0.10+7\bin;%PATH%"
)

rem Auto-detect portable Android SDK if installed in workspace
if exist "%SCRIPT_DIR%android-sdk" (
    set "ANDROID_HOME=%SCRIPT_DIR%android-sdk"
)

echo [1/2] Checking build environment...
echo Java: %JAVA_HOME%
echo SDK:  %ANDROID_HOME%
echo.

echo [2/2] Running Gradle assembleDebug to compile production APK...
call "%SCRIPT_DIR%gradlew.bat" assembleDebug

if %errorlevel% equ 0 (
    echo.
    echo ======================================================================
    echo   BUILD SUCCESSFUL!
    echo   Copying APK to project root...
    copy /y "%SCRIPT_DIR%app\build\outputs\apk\debug\app-debug.apk" "%SCRIPT_DIR%LOF_Titan_EOL_Test_Fixture.apk"
    echo   Your APK is ready at:
    echo   %SCRIPT_DIR%LOF_Titan_EOL_Test_Fixture.apk
    echo ======================================================================
    echo.
    explorer /select,"%SCRIPT_DIR%LOF_Titan_EOL_Test_Fixture.apk"
) else (
    echo.
    echo [ERROR] Build failed. Please check Gradle output above.
)

pause
endlocal
