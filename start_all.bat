@echo off
setlocal enabledelayedexpansion
title GOVIA - Start All
cd /d "%~dp0"

echo ===============================================
echo   GOVIA - Khoi dong Postgres + Backend + Frontend
echo ===============================================
echo.

REM --- 1. Dam bao Docker Desktop dang chay ---
docker info >nul 2>&1
if errorlevel 1 (
    echo [1/4] Docker chua chay, dang mo Docker Desktop...
    start "" "C:\Program Files\Docker\Docker\Docker Desktop.exe"
    echo Doi Docker khoi dong ^(toi da 90 giay^)...
    set count=0
    :waitdocker
    timeout /t 3 /nobreak >nul
    set /a count+=1
    docker info >nul 2>&1
    if errorlevel 1 (
        if !count! LSS 30 goto waitdocker
        echo.
        echo [LOI] Docker khong khoi dong duoc sau 90 giay.
        echo Hay mo Docker Desktop thu cong, doi no chay xong roi bam lai file nay.
        pause
        exit /b 1
    )
) else (
    echo [1/4] Docker da san sang.
)
echo.

REM --- 2. Khoi dong Postgres ---
echo [2/4] Dang khoi dong Postgres...
docker compose up -d
if errorlevel 1 (
    echo [LOI] Khong khoi dong duoc Postgres. Xem log Docker Desktop.
    pause
    exit /b 1
)
echo.

REM --- 3. Khoi dong Backend (cua so rieng, cong 8081) ---
echo [3/4] Dang khoi dong Backend Spring Boot ^(cong 8081^)...
start "GOVIA Backend" cmd /k "cd /d %~dp0backend\govia-identity && ..\mvnw.cmd spring-boot:run"
echo.

REM --- 4. Khoi dong Frontend (cua so rieng, cong 5173) ---
echo [4/4] Dang khoi dong Frontend Vite ^(cong 5173^)...
start "GOVIA Frontend" cmd /k "cd /d %~dp0frontend && npm run dev:shell"
echo.

echo Dang doi Backend/Frontend khoi dong xong ^(khoang 30 giay^) roi mo trinh duyet...
timeout /t 30 /nobreak >nul
start "" "http://localhost:5173"

echo.
echo ===============================================
echo Xong! Backend va Frontend dang chay trong 2 cua
echo so rieng (GOVIA Backend / GOVIA Frontend).
echo Dong 2 cua so do la se dung server.
echo Can test API truc tiep thi mo: http://localhost:8081/swagger-ui.html
echo ===============================================
pause
