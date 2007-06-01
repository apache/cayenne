@echo off

rem -------------------------------------------------------------------
rem     Windows batch script to run Cayenne Data View Modeler
rem
rem  Certain parts are modeled after Tomcat startup scrips, 
rem  Copyright Apache Software Foundation
rem -------------------------------------------------------------------

rem -------------------------------------------------------------------
rem  Variables:
rem -------------------------------------------------------------------

set MAIN_CLASS=org.objectstyle.cayenne.dataview.dvmodeler.Main


if not "%JAVA_HOME%" == "" goto check_cayenne_home
echo Please define JAVA_HOME to point to your JSDK installation.
goto eof


:check_cayenne_home
if not "%CAYENNE_HOME%" == "" goto got_home
set CAYENNE_HOME=..

:got_home
if exist "%CAYENNE_HOME%\bin\dvmodeler.bat" goto check_cp
echo Please define CAYENNE_HOME to point to your Cayenne installation.
goto eof


:check_cp
set JAVACMD=%JAVA_HOME%\bin\javaw
set DEFAULT_CLASSPATH=%CAYENNE_HOME%\lib\dvmodeler\cayenne-dvmodeler.jar

:run_modeler
start "DVModeler" "%JAVACMD%" -cp "%CLASSPATH%;%DEFAULT_CLASSPATH%" %MAIN_CLASS% %*

:eof
