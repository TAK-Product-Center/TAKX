@echo off

REM Read the vmoptions file
REM The results are placed into the variable JVM_OPTS
REM
REM USAGE: call read_vmoptions

REM NOTE: ENABLEDELAYEDEXPANSION must be set for this script to function properly

set JVM_OPTS=

set OPTIONS_FILE=raptor64.vmoptions

if not exist %OPTIONS_FILE% (
   REM Try to fix tings up in the development environment (the only place this should happen)

   set VMOPT_SOURCE=template-64bit.vmoptions

   echo %OPTIONS_FILE% file missing, creating one from template: !VMOPT_SOURCE!
   copy !VMOPT_SOURCE! %OPTIONS_FILE% >NUL
)

REM Now try again

if not exist %OPTIONS_FILE% (
   echo Missing options file: %OPTIONS_FILE%
   echo You should copy in the proper template for this architecture
   goto :FAIL
)

for /F "delims=@" %%P IN (%OPTIONS_FILE%) do (
   if "!JVM_OPTS!" == "" (
      set JVM_OPTS="%%P"
   ) else (
      set JVM_OPTS=!JVM_OPTS! "%%P"
   )
)

exit /b 0

:FAIL
rem Exit with an error level of 2 to indicate that we failed
exit /b 2
