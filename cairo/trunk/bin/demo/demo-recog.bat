@echo off

set PACKAGE=org.speechforge.cairo.demo.recog
set CLASS=RecognitionClient
set GRAMMAR_URL=file:../grammar/example.gram
set LOCAL_RTP_PORT=42046

start "%CLASS% - %GRAMMAR%" ..\..\bin\launch %PACKAGE%.%CLASS% "%GRAMMAR_URL%" %LOCAL_RTP_PORT%
