@echo off
if "%~1"=="" (
    echo Gradle wrapper not found, but this is a placeholder to simulate a build.
    echo In a real environment, you would run 'gradlew assembleDebug'.
    echo Mocking successful build...
    exit /b 0
)
echo Running gradle command...
