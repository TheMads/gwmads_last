@echo off
color 0b

SETLOCAL ENABLEDELAYEDEXPANSION
for %%a in ("*.jar") do (
	SET JARS=!JARS!;%%a
)
@java -cp %JARS:~1% ru.l2gw.gsregistering.GameServerRegister
ENDLOCAL
@pause