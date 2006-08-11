@echo off

set PACKAGE=org.speechforge.cairo.demo.tts
set CLASS=SpeechSynthClient
set PROMPT_TEXT=I am the very model of a modern major general.  I've information vegetable, animal and mineral.
set LOCAL_RTP_PORT=42046

start "%CLASS% - %GRAMMAR%" ..\..\bin\launch %PACKAGE%.%CLASS% "%PROMPT_TEXT%" %LOCAL_RTP_PORT%
