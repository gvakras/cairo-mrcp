===================================
Readme for Cairo Client v${project.version}
===================================

Overview
--------

Cairo client provides an open source speech resource client library written entirely in the Java programming language.  To achieve maximum compatibility with existing and future speech servers it has been designed from the ground up to comply with the MRCPv2 standard. (For more information on MRCPv2 please see http://www.ietf.org/internet-drafts/draft-ietf-speechsc-mrcpv2-14.txt)

Cairo client can be used to build mrcpv2 clients


Limitations for Cairo v${project.version}
--------------------------

This is the first release of cairo-client.  See General limitation section.


General limitations of this release:
------------------------------------

Limitations of the SIP support

   * Re-invite not implemented

   * Register method not implemented.  Cairo server does not register itself with a registrar.  Client must know the server's address.

   * Security (authentication and encryption/SIPS) not implemented

   * Options method not implemented

Limitations for the speechrecog resource:

  * A simple Semantic interpretation of the Recognition results are provided.  Natural Language Semantics Markup Language (NLSML) is not yet supported.

  * Only Java Speech Grammar Format (JSGF) is supported for specifying grammars to be used for recognition. Speech Recognition Grammar Specification (SRGS) is not yet supported.

Limitations for the speechsynth resource:

  * Text-to-speech (TTS) requests must be specified using plain text.  Speech Synthesis Markup Language (SSML) is not yet supported.


Prerequisites
-------------

Cairo requires Java Runtime Environment (JRE) 5.0 or higher which can be downloaded here:

  http://java.sun.com/javase/downloads/

If you have not already, you will need to set your JAVA_HOME environment variable to point to the installed location of your JRE/JDK.


Installation
------------

1. Extract Cairo-client

  To install Cairo, extract all files from the binary distribution archive to a directory of your choosing.

2. Download and Install JMF 2.1.1

  Cairo requires Java Media Framework (JMF) version 2.1.1. which can be downloaded here:

  http://java.sun.com/products/java-media/jmf/2.1.1/download.html

  Download and run the JMF installer that corresponds to your specific operating system.  This will install jmf.jar and sound.jar to the lib/ext directory of your installed JRE(s) as well as performing the configurations specific to your operating system.

3. Install JSAPI

  Run the JSAPI installer found in the lib directory of your Cairo installation (either jsapi.exe or jsapi.sh depending upon your operating system) and accept the Sun Microsystems license agreement.  This will extract the jsapi.jar to your Cairo lib directory.
  (If you run the JSAPI installer from a different directory you will need to move the jsapi.jar from that directory to your Cairo installation's lib directory in order for it to be included in the Cairo classpath.)

  Note: Extracting jsapi.jar to the lib directory is sufficient for this single Cairo installation.  However to avoid this step during future installations you can permanently install JSAPI by moving the jsapi.jar to the lib/ext directory of your installed JRE(s).

4.  Install cairo-server (or other mrcpv2 server)


Getting Started
---------------

Once Cairo-client is successfully installed (and you have a mrcpv2 server --like cairo-server) you can follow the instructions below to launch the  Cairo client demo MRCPv2 client.

Notes: These instructions and the batch files provided in the Cairo-server distribution are geared towards a deployment of Cairo on Windows.  Since Cairo is written entirely in Java it should work on any operating system, however it has, as of yet, only been verified on Windows.  If you are interested in deploying Cairo on another operating system please post a message to the cairo-user forum (http://www.nabble.com/cairo-user-f15778.html) to get assistance in this process.
       You must run a mrcpv2 server for the demo.  If you are using cairo-server, see the cairo-server installation instructions for starting up the server processes.


Library Overview
----------------

1.  Set up the session using sip (use cairo-sip or other sip libraries)
2   configure rtp if needed (use cairo-rtp or other rtp libraries)
3.  construct an implementation of speechClient
4.  Use speech client for recognition and sythesis
5.  Close session and release resources 

The SpeechClient Interfaces provides blocking and no-blocking methods.  

Use the SpeechEventListener interface to receive speech events when using non-blocking calls.


Running the Demo MRCPv2 Clients
-------------------------------

A number of demo clients are supplied with the Cairo Client installation.  These can either be run directly or they may be used as example code to write your own MRCPv2 client.

1. Prerequisites

  The included demo clients play synthesized speech and/or perform speech recognition on microphone input and as such require a (preferably high quality) microphone and speakers to be attached to the system executing the demos.

2. Example Grammar

  Most of the demo clients that include speech recognition functionality are configured by default to use the grammar file demo/grammar/example.gram. This grammar is in Java Speech Grammar Format (JSGF). If you are familiar with JSGF you can examine the grammar file to find out what some valid utterances are that the demos will recognize. Here are some examples of valid utterances from the example.gram grammar file:

    * "I would like sports news."
    * "Get me the weather."
    * "I would like to hear a stock quote."

  Demo clients that run in -loop mode use the grammar file demo/grammar/example-loop.gram instead. This grammar extends example.gram by adding voice commands for exiting a looping demo. The example-loop.gram grammar file adds the following recognized utterances:

    * "Exit."
    * "Quit."

3. Available Clients

  The following demo clients are included in the Cairo installation.

  +------------------+------------------------------------------------------------------------+
  | Client           | Description                                                            |
  +------------------+------------------------------------------------------------------------+
  | demo-bargein     | MRCPv2 client application that plays a TTS prompt while                |
  |                  | performing speech recognition on microphone input.  Prompt             |
  |                  | playback is cancelled as soon as start of speech is detected.          |
  +------------------+------------------------------------------------------------------------+
  | demo-parrot      | This is the same client as demo-bargein but run in -parrot mode        |
  |                  | so that recognized utterances are read back to the user using TTS.     |
  |                  |                                                                        |
  |                  | (Note: -loop mode is also enabled so that this demo will repeat        |
  |                  | until the phrase "quit" or "exit" is recognized.)                      |
  +------------------+------------------------------------------------------------------------+


  Each client can be started by running the appropriate batch script located in the demo/bin directory of your Cairo installation.  Source code for the demos is also included in the installation and can be found in the demo/src/java directory.

  Be sure to have a mrcpv2 server (like cairo-sercer) running before starting a client.

Further Information
-------------------

For more information please see the Cairo Project Home at http://cairo.speechforge.org.





+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
+ Copyright (C) 2005-2008 SpeechForge. All Rights Reserved. +
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 

