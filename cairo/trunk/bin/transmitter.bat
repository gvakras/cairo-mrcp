@echo off

set FREETTS_HOME=C:\root\java\freetts-1.2.1

set CLASS=org.speechforge.cairo.server.resource.TransmitterResource
set CAIRO_CONFIG=file:config/cairo-config.xml
set RES_NAME=transmitter1

start "%RES_NAME%" xlaunch %CLASS% "%CAIRO_CONFIG%" "%RES_NAME%"




set CLASSPATH=.
set CLASSPATH=%CLASSPATH%;%FREETTS_HOME%/lib/cmudict04.jar
set CLASSPATH=%CLASSPATH%;%FREETTS_HOME%/lib/cmulex.jar
set CLASSPATH=%CLASSPATH%;%FREETTS_HOME%/lib/cmutimelex.jar
set CLASSPATH=%CLASSPATH%;%FREETTS_HOME%/lib/cmu_time_awb.jar
set CLASSPATH=%CLASSPATH%;%FREETTS_HOME%/lib/cmu_us_kal.jar
set CLASSPATH=%CLASSPATH%;%FREETTS_HOME%/lib/en_us.jar
set CLASSPATH=%CLASSPATH%;%FREETTS_HOME%/lib/freetts.jar
