=======================================================
Readme for cairo-rtp, a rtp library for Speech v${project.version}
=======================================================

Overview
--------

Cairo-rtp provides a rtp libarary that is useful for building MRCPv2 compliant speech server solutions.  This libary can be useful for the client and server side processing.

Cairo-rtp is written entirely in the Java programming language and uses the Java Media Framework (JMF).

Limitations for Cairo v${project.version}
--------------------------

This is the first release of cairo-rtp.  See General limitation section.

General limitations of this release:
------------------------------------


Prerequisites
-------------

Cairo-rtp requires Java Runtime Environment (JRE) 5.0 or higher which can be downloaded here:

  http://java.sun.com/javase/downloads/

If you have not already, you will need to set your JAVA_HOME environment variable to point to the installed location of your JRE/JDK.


Installation
------------

1. Extract Cairo-rtp

2. Download and Install JMF 2.1.1

  Cairo requires Java Media Framework (JMF) version 2.1.1. which can be downloaded here:

  http://java.sun.com/products/java-media/jmf/2.1.1/download.html

  Download and run the JMF installer that corresponds to your specific operating system.  This will install jmf.jar and sound.jar to the lib/ext directory of your installed JRE(s) as well as performing the configurations specific to your operating system.

3. Install JSAPI

  Run the JSAPI installer found in the lib directory of your Cairo installation (either jsapi.exe or jsapi.sh depending upon your operating system) and accept the Sun Microsystems license agreement.  This will extract the jsapi.jar to your Cairo lib directory.
  (If you run the JSAPI installer from a different directory you will need to move the jsapi.jar from that directory to your Cairo installation's lib directory in order for it to be included in the Cairo classpath.)

  Note: Extracting jsapi.jar to the lib directory is sufficient for this single Cairo installation.  However to avoid this step during future installations you can permanently install JSAPI by moving the jsapi.jar to the lib/ext directory of your installed JRE(s).



Getting Started
---------------

cairo-rtp is not a standalone installation.  It is a library that is used by cairo-server, zanzibar and cairo-client.  If you wish to use it your self in your own applciation, include the jar in your class path.
   
A good place to start would be to look at the the exmaple demo in cairo-client.

Further Information
-------------------

For more information please see the Cairo Project Home at http://cairo.speechforge.org.


+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
+ Copyright (C) 2005-2008 SpeechForge. All Rights Reserved. +
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 

