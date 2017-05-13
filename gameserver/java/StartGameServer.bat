@echo off
SETLOCAL ENABLEDELAYEDEXPANSION
:start
TITLE L2GW: High Five Game Server
echo Starting L2GW: High Five Game Server.
echo.

for %%a in ("*.jar") do (
	SET JARS=!JARS!;%%a
)

rem ======== Optimize memory settings =======
rem Minimal size with geodata is 1.5G, w/o geo 1G
rem Make sure -Xmn value is always 1/4 the size of -Xms and -Xmx.
rem -Xms and -Xmx should always be equal.
rem ==========================================
java -Dfile.encoding=UTF-8 -Xincgc -Xms1072m -Xmx1572m -cp %JARS:~1% ru.l2gw.gameserver.GameServer
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
del gameserver_is_running.tmp
pause
ENDLOCAL
