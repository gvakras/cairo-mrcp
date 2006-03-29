@echo off
echo ***************************************
echo *  Make sure JAVA_HOME has been set!  *
echo ***************************************

set CAIRO_VERSION=SNAPSHOT
set MVN_REPOSITORY=%USERPROFILE%\.m2\repository

set CLASSPATH=.
set CLASSPATH=%CLASSPATH%;%MVN_REPOSITORY%\com\onomatopia\cairo\%CAIRO_VERSION%\cairo-%CAIRO_VERSION%.jar
set CLASSPATH=%CLASSPATH%;%MVN_REPOSITORY%\org\mrcp4j\mrcp4j\0.1\mrcp4j-0.1.jar
set CLASSPATH=%CLASSPATH%;%MVN_REPOSITORY%\commons-collections\commons-collections\3.1\commons-collections-3.1.jar
set CLASSPATH=%CLASSPATH%;%MVN_REPOSITORY%\commons-configuration\commons-configuration\1.2\commons-configuration-1.2.jar
set CLASSPATH=%CLASSPATH%;%MVN_REPOSITORY%\commons-lang\commons-lang\2.1\commons-lang-2.1.jar
set CLASSPATH=%CLASSPATH%;%MVN_REPOSITORY%\commons-pool\commons-pool\1.2\commons-pool-1.2.jar
set CLASSPATH=%CLASSPATH%;%MVN_REPOSITORY%\jdom\jdom\1.0\jdom-1.0.jar
set CLASSPATH=%CLASSPATH%;%MVN_REPOSITORY%\log4j\log4j\1.2.13\log4j-1.2.13.jar
set CLASSPATH=%CLASSPATH%;%MVN_REPOSITORY%\directory-network\mina\mina-0.7.2.jar
set CLASSPATH=%CLASSPATH%;%MVN_REPOSITORY%\sphinx\sphinx4\1.0beta\sphinx4-1.0beta.jar
set CLASSPATH=%CLASSPATH%;%MVN_REPOSITORY%\sphinx\WSJ_8gau_13dCep_16k_40mel_130Hz_6800Hz\1.0beta\WSJ_8gau_13dCep_16k_40mel_130Hz_6800Hz-1.0beta.jar

set CLASS=com.onomatopia.cairo.server.recog.sphinx.SphinxRecEngine$Test

%JAVA_HOME%\bin\java -cp "%CLASSPATH%" -Xmx200m -Dlog4j.configuration=log4j.xml %CLASS%
pause