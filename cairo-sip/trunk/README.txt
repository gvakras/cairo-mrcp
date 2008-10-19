=======================================================
Readme for cairo-sip, a Sip Library for Speech v${project.version}
=======================================================

Overview
--------

Cairo-sip provides a sip libarary that is useful for building enterprise grade, MRCPv2 compliant speech server solutions.  
This libary can be useful for the client and server side processing.   SIP is an important part MRCPv2 system.  The following
is a direct quote from the MRCPv2 specification

   "MRCPv2 is not a "stand-alone" protocol - it relies on a session management protocol
   such as the Session Initiation Protocol (SIP) to establish the MRCPv2
   control session between the client and the server, and for rendezvous
   and capability discovery. It also depends on SIP and SDP to
   establish the media sessions and associated parameters between the
   media source or sink and the media server."


Cairo-sip is written entirely in the Java programming language.


Limitations for Cairo v${project.version}
--------------------------

This is the first release of cairo-sip.  See General limitation section.


General limitations of this release:
------------------------------------
   * Re-invite not implemented

   * Register method not implemented.  Cairo server does not register itself with a registrar.  Client must know the server's address.

   * Security (authentication and encryption/SIPS) not implemented

   * Options method not implemented


Prerequisites
-------------

Cairo-sip requires Java Runtime Environment (JRE) 5.0 or higher which can be downloaded here:

  http://java.sun.com/javase/downloads/

If you have not already, you will need to set your JAVA_HOME environment variable to point to the installed location of your JRE/JDK.


Installation
------------

1. Extract Cairo-sip


Getting Started
---------------

cairo-sip is not a standalone installation.  It is a library that is used by cairo-server, zanzibar and cairo-client.  IF you wish to use it your self in your own applciation, include the jar in your class path.
   
Perhaps a good place to start would be to look at the the exmaple demo in cairo-client.


Further Information
-------------------

For more information please see the Cairo Project Home at http://cairo.speechforge.org.


+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
+ Copyright (C) 2005-2008 SpeechForge. All Rights Reserved. +
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 

