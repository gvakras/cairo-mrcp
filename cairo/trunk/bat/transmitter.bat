@echo off
set SOURCE_DIR=..
set THIRDPARTY=%SOURCE_DIR%\..\..\thirdparty
set JAVA_HOME=C:\java\jdk1.5.0
set FREETTS_HOME=C:\java\freetts-1.2.1

set CLASSPATH=.
set CLASSPATH=%CLASSPATH%;%THIRDPARTY%\gen\cairo.jar
set CLASSPATH=%CLASSPATH%;%THIRDPARTY%\gen\mrcp4j.jar
set CLASSPATH=%CLASSPATH%;%THIRDPARTY%\commons-collections-3.1.jar
set CLASSPATH=%CLASSPATH%;%THIRDPARTY%\commons-configuration-1.1.jar
set CLASSPATH=%CLASSPATH%;%THIRDPARTY%\commons-lang-2.1.jar
set CLASSPATH=%CLASSPATH%;%THIRDPARTY%\commons-logging-1.0.4.jar
set CLASSPATH=%CLASSPATH%;%THIRDPARTY%\commons-pool-1.2.jar
set CLASSPATH=%CLASSPATH%;%THIRDPARTY%\JainSipApi1.1.jar
set CLASSPATH=%CLASSPATH%;%THIRDPARTY%\jdom.jar
set CLASSPATH=%CLASSPATH%;%THIRDPARTY%\jmf.jar
set CLASSPATH=%CLASSPATH%;%THIRDPARTY%\log4j-1.2.12.jar
set CLASSPATH=%CLASSPATH%;%THIRDPARTY%\mina-0.7.2.jar
set CLASSPATH=%CLASSPATH%;%THIRDPARTY%\nist-sip-1.2.jar
set CLASSPATH=%CLASSPATH%;%THIRDPARTY%\servlet-api.jar
set CLASSPATH=%CLASSPATH%;%FREETTS_HOME%/lib/cmudict04.jar
set CLASSPATH=%CLASSPATH%;%FREETTS_HOME%/lib/cmulex.jar
set CLASSPATH=%CLASSPATH%;%FREETTS_HOME%/lib/cmutimelex.jar
set CLASSPATH=%CLASSPATH%;%FREETTS_HOME%/lib/cmu_time_awb.jar
set CLASSPATH=%CLASSPATH%;%FREETTS_HOME%/lib/cmu_us_kal.jar
set CLASSPATH=%CLASSPATH%;%FREETTS_HOME%/lib/en_us.jar
set CLASSPATH=%CLASSPATH%;%FREETTS_HOME%/lib/freetts.jar

set CLASS=org.speechforge.cairo.server.resource.TransmitterResource
set CAIRO_CONFIG=file:///C:/cvs/sf/cairo/config/cairo-config.xml
set RES_NAME=output

java -cp %CLASSPATH% -Dlog4j.configuration=log4j.xml %CLASS% "%CAIRO_CONFIG%" "%RES_NAME%"