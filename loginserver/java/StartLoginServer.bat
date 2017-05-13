@echo off
SETLOCAL ENABLEDELAYEDEXPANSION
:start
TITLE L2GW: High Five Login Server
echo %DATE% %TIME% Login server is running !!! > login_is_running.tmp
echo Starting L2GW: High Five Login Server.
echo.

for %%a in ("*.jar") do (
	SET JARS=!JARS!;%%a
)

java -Xms32m -Xmx32m -cp %JARS:~1% ru.l2gw.loginserver.L2LoginServer
if ERRORLEVEL 2 goto restart
if ERRORLEVEL 1 goto error
goto end
:restart
echo.
echo Admin Restart ...
echo.
goto start
:error
echo.
echo Server terminated abnormaly
echo.
:end
echo.
echo server terminated
echo.
del login_is_running.tmp
pause
ENDLOCAL