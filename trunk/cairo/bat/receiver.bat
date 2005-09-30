@echo off
set SOURCE_DIR=..
set THIRDPARTY=%SOURCE_DIR%\..\..\thirdparty
set JAVA_HOME=C:\java\jdk1.5.0
set SPHINX_HOME=C:\sphinx\sphinx4-1.0beta

set CLASSPATH=%THIRDPARTY%\gen\cairo.jar
set CLASSPATH=%CLASSPATH%;%THIRDPARTY%\gen\mrcp4j.jar
set CLASSPATH=%CLASSPATH%;%THIRDPARTY%\commons-collections-3.1.jar
set CLASSPATH=%CLASSPATH%;%THIRDPARTY%\commons-configuration-1.1.jar
set CLASSPATH=%CLASSPATH%;%THIRDPARTY%\commons-lang-2.1.jar
set CLASSPATH=%CLASSPATH%;%THIRDPARTY%\commons-logging-1.0.4.jar
set CLASSPATH=%CLASSPATH%;%THIRDPARTY%\commons-pool-1.2.jar
set CLASSPATH=%CLASSPATH%;%THIRDPARTY%\JainSipApi1.1.jar
set CLASSPATH=%CLASSPATH%;%THIRDPARTY%\jdom.jar
set CLASSPATH=%CLASSPATH%;%THIRDPARTY%\jmf.jar
set CLASSPATH=%CLASSPATH%;%THIRDPARTY%\mina-0.7.2.jar
set CLASSPATH=%CLASSPATH%;%THIRDPARTY%\nist-sip-1.2.jar
set CLASSPATH=%CLASSPATH%;%THIRDPARTY%\servlet-api.jar
set CLASSPATH=%CLASSPATH%;%SPHINX_HOME%\lib\sphinx4.jar
set CLASSPATH=%CLASSPATH%;%SPHINX_HOME%\lib\jsapi.jar
set CLASSPATH=%CLASSPATH%;%SPHINX_HOME%\lib\WSJ_8gau_13dCep_16k_40mel_130Hz_6800Hz.jar

set CLASS=com.onomatopia.cairo.server.resource.ReceiverResource
set CAIRO_CONFIG=file:///C:/work/cvs/onomatopia/cairo/config/cairo-config.xml
set RES_NAME=input
set SPHINX_CONFIG=file:///C:/work/cvs/onomatopia/cairo/config/sphinx-rtp.xml

java -cp %CLASSPATH% -Xmx200m %CLASS% "%CAIRO_CONFIG%" "%RES_NAME%" "%SPHINX_CONFIG%"