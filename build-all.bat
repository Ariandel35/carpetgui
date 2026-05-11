@echo off
setlocal enabledelayedexpansion

set OUTPUT_DIR=build\releases
mkdir %OUTPUT_DIR% 2>nul

:: Save current state
git stash push -m "build-all-auto-stash" 2>nul

:: 1.20.1 - JDK 17
echo ===== Building 1.20.1 =====
git checkout 1.20.1
if %errorlevel% neq 0 (
    echo Failed to checkout 1.20.1
    goto :cleanup
)
call .\gradlew build -Dorg.gradle.java.home="C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
if %errorlevel% equ 0 (
    copy build\libs\CarpetGUI-Rewrite-*.jar %OUTPUT_DIR%\ 2>nul
) else (
    echo Build failed for 1.20.1
)

:: 1.21.4 - JDK 21
echo ===== Building 1.21.4 =====
git checkout 1.21.4
if %errorlevel% neq 0 (
    echo Failed to checkout 1.21.4
    goto :cleanup
)
call .\gradlew build -Dorg.gradle.java.home="C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
if %errorlevel% equ 0 (
    copy build\libs\CarpetGUI-Rewrite-*.jar %OUTPUT_DIR%\ 2>nul
) else (
    echo Build failed for 1.21.4
)

:: main (26.1) - JDK 25
echo ===== Building main (26.1) =====
git checkout main
if %errorlevel% neq 0 (
    echo Failed to checkout main
    goto :cleanup
)
call .\gradlew build -Dorg.gradle.java.home="C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot"
if %errorlevel% equ 0 (
    copy build\libs\CarpetGUI-Rewrite-*.jar %OUTPUT_DIR%\ 2>nul
) else (
    echo Build failed for main
)

:cleanup
git checkout main 2>nul
git stash pop 2>nul
echo ===== Done =====
echo JARs in: %OUTPUT_DIR%
