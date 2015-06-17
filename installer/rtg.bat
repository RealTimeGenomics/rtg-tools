@echo off

setlocal ENABLEEXTENSIONS

REM --- START OF USER SETTINGS ---

REM   Amount of memory to allocate to RTG.  Use G suffix for gigabytes
REM   If not set, will default to 90% of available RAM
REM   for example, the following sets maximum memory to 4 GB
REM   SET RTG_MEM=4G
SET RTG_MEM=

REM   If RTG_MEM is not defined use this percentage of total RAM
SET RTG_MEM_PCT=90

REM   Attempt to send crash logs to Real Time Genomics, true to enable, false to disable
SET RTG_TALKBACK=true

REM   Enable simple usage logging, true to enable, false to disable. Default is no logging
SET RTG_USAGE=

REM   Server URL when usage logging to server. Default is to use RTG hosted server.
SET RTG_USAGE_HOST=

REM   If performing single-user file-based usage logging, this specifies the directory to log to.
SET RTG_USAGE_DIR=

REM   List of optional fields to add to usage logging (when enabled).
REM   If unset do not add any of these fields. (commandline may contain information 
REM   considered sensitive)
REM   SET RTG_USAGE_OPTIONAL=username,hostname,commandline
SET RTG_USAGE_OPTIONAL=username,hostname

REM   Proxy host if needed, blank otherwise
SET PROXY_HOST=

REM   Proxy port if needed, blank otherwise
SET PROXY_PORT=

REM   Directory in which to look for pipeline reference datasets
SET RTG_REFERENCES_DIR=

REM   Directory in which to look for pipeline reference datasets
SET RTG_MODELS_DIR=

REM   Additional JVM options (e.g.: "-Djava.io.tmpdir=XXYY -XX:+UseLargePages")
SET RTG_JAVA_OPTS=

REM   Set the number of threads to use when not otherwise specified via command line flags. 
REM   The default behavior is to allocate one thread per machine core.
SET RTG_DEFAULT_THREADS=



REM --- END OF USER SETTINGS ---


 
REM   location of RTG jar file
SET RTG_JAR="%~dp0RTG.jar"

REM   minimum memory used for RTG talkback/usage and other calls
SET RTG_MIN_MEM=64M

REM   path to java.exe
SET RTG_JAVA="%~dp0%jre\bin\java.exe"

REM check if we were started from windows explorer and provide a sensible message
echo %cmdcmdline% | find "cmd /c" >nul
if %ERRORLEVEL% NEQ 0 GOTO :start

SET PAUSE_ON_CLOSE=1
IF /I "%PROCESSOR_ARCHITECTURE%" NEQ "AMD64" GOTO :no64bit

echo RTG is a command line application, please follow the steps below
echo.  
echo 1) Open windows command prompt  
echo    e.g. on Windows Vista / 7, press start, search "cmd.exe" and press enter
echo.  
echo 2) Type "%~dp0%rtg.bat" to execute RTG command
echo.  
echo For more information please refer to "%~dp0%RTGOperationsManual.pdf"
echo.  
GOTO :no 
 
:start
IF /I "%PROCESSOR_ARCHITECTURE%" NEQ "AMD64" GOTO :no64bit

REM   Following checks for supported OS
REM   Windows XP 64 bit and Windows Server 2003
ver | FIND "5.2" > nul
IF %ERRORLEVEL% EQU 0 GOTO :os_ok

REM   Windows Vista and Windows Sever 2008 
ver | FIND "6.0" > nul
IF %ERRORLEVEL% EQU 0 GOTO :os_ok

REM   Windows 7 and Windows Sever 2008 R2 
ver | FIND "6.1" > nul
IF %ERRORLEVEL% EQU 0 GOTO :os_ok

REM   Windows 8 and Windows Sever 2012
ver | FIND "6.2" > nul
IF %ERRORLEVEL% EQU 0 GOTO :os_ok

REM   Windows 8.1 and Windows Sever 2012 R2 
ver | FIND "6.3" > nul
IF %ERRORLEVEL% EQU 0 GOTO :os_ok

REM   We have checked for all tested versions of Windows   
echo This is not a supported version of windows, please contact support@realtimegenomics.com for more information
GOTO :no

:os_ok

SET ACCEPTED_NAME="%~dp0%.license_accepted"

SET ACCEPTED_USAGE_NAME="%~dp0%.usage_accepted"

SET EULA="%~dp0%LICENSE.txt"

IF EXIST %ACCEPTED_NAME% GOTO :setvars

REM  Skip asking about the EULA if we havn't bundled it
IF NOT EXIST %EULA% GOTO :yes

more %EULA%

SET /P ANSWER=Do you agree to the terms and conditions (y/n)? 

IF /i {%ANSWER%}=={y} (GOTO :yes)
IF /i {%ANSWER%}=={yes} (GOTO :yes)

echo You must agree with the license terms before you can use the software.
GOTO :no

:yes

echo %date% - %time% >%ACCEPTED_NAME%

REM   First run only, perform test of crash-reporting
%RTG_JAVA% %PROXY_HOST% %PROXY_PORT% -Xmx%RTG_MIN_MEM% -cp %RTG_JAR% com.rtg.util.diagnostic.SimpleTalkback "Post-install talkback test"
IF %ERRORLEVEL% NEQ 0 (
     echo Initial crash-report connectivity test did not succeed, probably due to firewall issues.
     echo You will be asked to manually submit any error logs.
)
echo.

:usage
echo RTG has a facility to automatically send basic usage information to Real
echo Time Genomics. This does not contain confidential information such as
echo command-line parameters or dataset contents.
echo.
SET /P USAGE_ANSWER=Would you like to enable automatic simple usage reporting (y/n)? 
echo.
IF /i {%USAGE_ANSWER%}=={y} (GOTO :usage_yes)
IF /i {%USAGE_ANSWER%}=={yes} (GOTO :usage_yes)

GOTO :usage_no

:usage_yes
<nul (SET /P FOO=true) >%ACCEPTED_USAGE_NAME%
GOTO :firstrun_end

:usage_no
echo Automatic usage reporting disabled.
echo.
<nul (SET /P FOO=false) >%ACCEPTED_USAGE_NAME%
GOTO :firstrun_end

:no64bit
echo RTG requires a 64 bit version of Windows

:no
IF DEFINED PAUSE_ON_CLOSE pause
exit /b 1

:firstrun_end

echo Initial configuration complete.  Advanced user configuration is 
echo available by editing settings in rtg.bat
echo.

:setvars

SET RTG_TALKBACK=-Dtalkback=%RTG_TALKBACK%

IF "%RTG_USAGE%" == "" (
    IF EXIST %ACCEPTED_USAGE_NAME% SET /P USAGE_ANSWER=<%ACCEPTED_USAGE_NAME%
) ELSE (
    SET USAGE_ANSWER=%RTG_USAGE%
)
SET RTG_USAGE=-Dusage=%USAGE_ANSWER%
IF NOT "%RTG_DEFAULT_THREADS%" == "" SET RTG_DEFAULT_THREADS=-Druntime.defaultThreads=%RTG_DEFAULT_THREADS%
IF NOT "%RTG_USAGE_HOST%" == "" SET RTG_USAGE=%RTG_USAGE% -Dusage.host=%RTG_USAGE_HOST%
IF NOT "%RTG_USAGE_DIR%" == "" SET RTG_USAGE=%RTG_USAGE% "-Dusage.dir=%RTG_USAGE_DIR%"
IF "%RTG_USAGE_OPTIONAL%" == "" (GOTO :skipoptional)
IF NOT "x%RTG_USAGE_OPTIONAL:username=x%" == "x%RTG_USAGE_OPTIONAL%" SET RTG_USAGE=%RTG_USAGE% -Dusage.log.username=true
IF NOT "x%RTG_USAGE_OPTIONAL:hostname=x%" == "x%RTG_USAGE_OPTIONAL%" SET RTG_USAGE=%RTG_USAGE% -Dusage.log.hostname=true
IF NOT "x%RTG_USAGE_OPTIONAL:commandline=x%" == "x%RTG_USAGE_OPTIONAL%" SET RTG_USAGE=%RTG_USAGE% -Dusage.log.commandline=true
:skipoptional

IF "%RTG_REFERENCES_DIR%" == "" (
    SET RTG_REFERENCES_DIR="-Dreferences.dir=%~dp0%references"
) ELSE (
    SET RTG_REFERENCES_DIR="-Dreferences.dir=%RTG_REFERENCES_DIR%"
)

IF "%RTG_MODELS_DIR%" == "" (
    SET RTG_MODELS_DIR="-Dmodels.dir=%~dp0%models"
) ELSE (
    SET RTG_MODELS_DIR="-Dmodels.dir=%RTG_MODELS_DIR%"
)

IF NOT "%PROXY_HOST%" == "" SET PROXY_HOST=-Dproxy.host=%PROXY_HOST%
IF NOT "%PROXY_PORT%" == "" SET PROXY_PORT=-Dproxy.port=%PROXY_PORT%

REM set memory
IF "%RTG_MEM%" == "" (
    FOR /F "usebackq" %%A in (`CALL %RTG_JAVA% -cp %RTG_JAR% com.rtg.util.ChooseMemory %RTG_MEM_PCT%`) DO SET RTG_MEM=%%A
)

%RTG_JAVA% -Xmx%RTG_MEM% %RTG_JAVA_OPTS% %RTG_REFERENCES_DIR% %RTG_MODELS_DIR% %RTG_USAGE% %RTG_TALKBACK% %PROXY_HOST% %PROXY_PORT% %RTG_DEFAULT_THREADS% -jar %RTG_JAR% %*

:exit_zero
exit /b 0
