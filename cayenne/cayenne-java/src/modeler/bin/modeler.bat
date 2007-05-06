@echo off

rem -------------------------------------------------------------------
rem     Windows batch script to run Cayenne Modeler
rem
rem  Certain parts are modeled after Tomcat startup scrips, 
rem  Copyright Apache Software Foundation
rem -------------------------------------------------------------------

rem -------------------------------------------------------------------
rem  Variables:
rem -------------------------------------------------------------------

rem -------------------------------------------------------------------
rem  Known problems:
rem
rem  If you get an "Out Of Environment Space" error under win9x,
rem  try replacing reference to MAIN_CLASS, JAVACMD, and DEFAULT_CLASSPATH
rem  with the literal contents of those values,
rem  or raise your default environment space from 256 characters to 4096 characters.
rem -------------------------------------------------------------------

set MAIN_CLASS=org.objectstyle.cayenne.modeler.Main

set CLASSPATH=

if not "%JAVA_HOME%" == "" goto check_cayenne_home
echo Please define JAVA_HOME to point to your JSDK installation.
goto eof


:check_cayenne_home
if not "%CAYENNE_HOME%" == "" goto got_home
set CAYENNE_HOME=..

:got_home
if exist "%CAYENNE_HOME%\bin\modeler.bat" goto check_cp
echo Please define CAYENNE_HOME to point to your Cayenne installation.
goto eof


:check_cp
set JAVACMD=%JAVA_HOME%\bin\javaw
set DEFAULT_CLASSPATH=%CAYENNE_HOME%\lib\modeler\cayenne-modeler.jar

:run_modeler

if "%os%" == "" goto win9x
goto winNT

:win9x
start "%JAVACMD%" -cp "%DEFAULT_CLASSPATH%" %MAIN_CLASS% %*
goto end

:winNT
start "CayenneModeler" "%JAVACMD%" -cp "%DEFAULT_CLASSPATH%" %MAIN_CLASS% %*
goto end

:end 

:eof
