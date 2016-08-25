
set svc_username=%1
set svc_password=%2

CALL D:\stp\app\instances\\${jvm.jvmName}\bin\setenv.bat

ECHO Install the service
CMD /C D:\stp\apache-tomcat-7.0.55\core\bin\service.bat install ${jvm.jvmName}

ECHO Update Java Options
CMD /C D:\stp\apache-tomcat-7.0.55\core\bin\tomcat7 //US//${jvm.jvmName} ++JvmOptions %JAVA_SERVICE_OPTS% --StartPath %START_PATH% --StdOutput "" --StdError ""

ECHO Change the service to automatically start
SC CONFIG ${jvm.jvmName} start= auto

if %svc_username%=="" goto :no_user

SC CONFIG ${jvm.jvmName} obj=%svc_username% password=%svc_password%

:no_user

EXIT %ERRORLEVEL%
