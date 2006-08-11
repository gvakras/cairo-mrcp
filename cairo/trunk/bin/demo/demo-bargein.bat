@echo off

set PACKAGE=org.speechforge.cairo.demo.bargein
set CLASS=BargeInClient
set GRAMMAR_URL=file:../grammar/example.gram
set PROMPT_TEXT=You can start speaking any time.  Would you like to hear the weather, get sports news or hear a stock quote?
set LOCAL_RTP_PORT=42046

start "%CLASS% - %GRAMMAR%" ..\..\bin\launch %PACKAGE%.%CLASS% "%GRAMMAR_URL%" "%PROMPT_TEXT%" %LOCAL_RTP_PORT%
