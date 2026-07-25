@echo off
REM scripts\release.bat — atlas-richie-ai 发布脚本（Windows / IDEA 友好版）
REM
REM 用法：
REM   scripts\release.bat snapshot    部署 SNAPSHOT 到 CNB
REM   scripts\release.bat release     部署 release 到 Maven Central
REM   scripts\release.bat dry-run     只跑 verify
REM
REM 在 IDEA 里配置：
REM   Run → Edit Configurations → "+" → Shell Script
REM     Script path : <项目根>/scripts/release.bat
REM     Script options: snapshot    （或 release）
REM
REM 本项目已回退到 Maven 3 写法（modelVersion 4.0.0 + 硬编码版本号）。
REM 推荐安装 mvn3.bat（Maven 3.9.x）并放在 PATH 前面，避免 Maven 4 JdkTransporter
REM 与 CNB auth 的兼容问题。脚本优先调用 mvn3。

setlocal EnableDelayedExpansion

set "MODE=%~1"

REM 优先 mvn3（Maven 3.9.x wagon 稳定，CNB 部署无 JdkTransporter auth bug），
REM fallback mvn（系统 PATH 上的 Maven，默认是 4.0.0-rc-5）。
set "MVN=mvn"
where mvn3 >nul 2>nul
if not errorlevel 1 set "MVN=mvn3"

if "%MODE%"=="snapshot" goto :snapshot
if "%MODE%"=="release"  goto :release
if "%MODE%"=="dry-run"  goto :dryrun

echo Usage: %~n0 {snapshot^|release^|dry-run}
exit /b 1

:snapshot
echo ==^> [snapshot] Deploying SNAPSHOT to CNB private server
echo     Target : https://maven.cnb.cool/richie696/repo-richie-snapshot/
echo     GPG    : SKIPPED (-Dgpg.skip=true)
echo     MVN    : %MVN%
echo.
where %MVN% >nul 2>nul
if errorlevel 1 (
    echo [ERROR] %MVN% not found in PATH.
    exit /b 1
)
%MVN% -DskipTests -Dgpg.skip=true clean deploy
if errorlevel 1 exit /b 1
echo.
echo ==^> [snapshot] DONE.
goto :end

:release
echo ==^> [release] Deploying release to Maven Central
echo     Target : Maven Central
echo     GPG    : REQUIRED
echo     -Dmaven.deploy.skip=true  Skip maven-deploy-plugin to avoid double-publish
echo     MVN    : %MVN%
echo.
where %MVN% >nul 2>nul
if errorlevel 1 (
    echo [ERROR] %MVN% not found in PATH.
    exit /b 1
)
where gpg >nul 2>nul
if errorlevel 1 (
    echo [ERROR] gpg not found in PATH. Install gnupg.
    exit /b 1
)
%MVN% -DskipTests -Dmaven.deploy.skip=true clean deploy
if errorlevel 1 exit /b 1
echo.
echo ==^> [release] DONE.
goto :end

:dryrun
echo ==^> [dry-run] Running verify only
echo     MVN    : %MVN%
echo.
where %MVN% >nul 2>nul
if errorlevel 1 (
    echo [ERROR] %MVN% not found in PATH.
    exit /b 1
)
%MVN% -DskipTests -Dgpg.skip=true clean verify
if errorlevel 1 exit /b 1
echo.
echo ==^> [dry-run] DONE.

:end
endlocal