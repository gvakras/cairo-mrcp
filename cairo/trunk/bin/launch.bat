@echo off
@setlocal enableextensions enabledelayedexpansion

set CAIRO_VERSION=${pom.version}

@REM ==== START VALIDATION ====
if not "%1" == "" goto chkJavaHome

echo.
echo ERROR: improper call to launch script
echo launch.bat should not be executed directly, please see README for
echo proper application launching instructions.
echo.
goto error

:chkJavaHome

if not "%JAVA_HOME%" == "" goto valJavaHome

echo.
echo ERROR: JAVA_HOME not found in your environment.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation
echo.
goto error

:valJavaHome
if exist "%JAVA_HOME%\bin\java.exe" goto chkJMF

echo.
echo ERROR: JAVA_HOME is set to an invalid directory.
echo JAVA_HOME = %JAVA_HOME%
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation
echo.
goto error

:chkJMF
if exist "%JAVA_HOME%\jre\lib\ext\jmf.jar" goto chkJSAPI

echo.
echo ERROR: Java Media Framework (JMF) is not installed.
echo Please download and install JMF from Sun Java web site:
echo http://java.sun.com/products/java-media/jmf/
echo.
goto error

:chkJSAPI
if exist "%JAVA_HOME%\jre\lib\ext\jsapi.jar" goto init
if exist "..\lib\jsapi.jar" goto init

echo.
echo ERROR: Java Speech API (JSAPI) is not installed.
echo Please run jsapi.exe or jsapi.sh and place the extracted
echo jsapi.jar in %JAVA_HOME%\jre\lib\ext
echo The install file can be downloaded from here:
echo http://www.speechforge.org/downloads/jsapi
echo.
goto error

:chkCairoHome
if not "%CAIRO_HOME%"=="" goto valCairoHome

echo.
echo ERROR: CAIRO_HOME not found in your environment.
echo Please set the CAIRO_HOME variable in your environment to match the
echo location of the Cairo installation
echo.
goto error

:valMHome
if exist "%CAIRO_HOME%\bin\rserver.bat" goto init

echo.
echo ERROR: CAIRO_HOME is set to an invalid directory.
echo CAIRO_HOME = %CAIRO_HOME%
echo Please set the CAIRO_HOME variable in your environment to match the
echo location of the Cairo installation
echo.
goto error
@REM ==== END VALIDATION ====

:init
cd ..

set CLASSPATH=%CD%\cairo-%CAIRO_VERSION%.jar
for %%b in (lib\*.jar) do set CLASSPATH=!CLASSPATH!;%CD%\%%b

if "%FREETTS_HOME%" == "" goto skipFreeTTS
for %%b in (%FREETTS_HOME%\lib\*.jar) do set CLASSPATH=!CLASSPATH!;%%b

:skipFreeTTS
set CLASSPATH=%CLASSPATH%;%CD%\config
@REM 
echo CLASSPATH=%CLASSPATH%
goto run

:run
%JAVA_HOME%\bin\java -cp "%CLASSPATH%" -Xmx200m -Dlog4j.configuration=log4j.xml %1 %2 %3
goto exit

:error
if "%OS%"=="Windows_NT" @endlocal
set ERROR_CODE=1
pause

:exit
if "%OS%"=="Windows_NT" @endlocal
