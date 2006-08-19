@echo off

set PACKAGE=org.speechforge.cairo.demo.standalone
set CLASS=StandaloneRecogClient
set GRAMMAR_URL=file:../grammar/example.gram

start "%CLASS% - %GRAMMAR%" ..\..\bin\launch %PACKAGE%.%CLASS% "%GRAMMAR_URL%"
