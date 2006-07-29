@echo off

set PACKAGE=org.speechforge.cairo.demo.tts
set CLASS=SpeechSynthClient
@REM set PROMPT_TEXT=You can start speaking any time.  Would you like to hear the weather, get sports news or hear a stock quote?
set PROMPT_TEXT=I am the very model of a modern major general.  I've information vegetable, animal and mineral.
set LOCAL_RTP_PORT=42046

start "%CLASS% - %GRAMMAR%" ..\..\bin\launch %PACKAGE%.%CLASS% "%PROMPT_TEXT%" %LOCAL_RTP_PORT%
