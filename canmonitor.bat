@echo USAGE: canmonitor -p host -n node -f EDS_file

set JAVA_HOME=D:\app\j2sdk1.4.1

%JAVA_HOME%\bin\javaw.exe -classpath class;%JAVA_HOME%\jre\lib\charsets.jar;%JAVA_HOME%\jre\lib\jaws.jar;%JAVA_HOME%\jre\lib\jce.jar;%JAVA_HOME%\jre\lib\jsse.jar;%JAVA_HOME%\jre\lib\rt.jar;%JAVA_HOME%\jre\lib\sunrsasign.jar;%JAVA_HOME%\jre\lib\ext\dnsns.jar;%JAVA_HOME%\jre\lib\ext\ldapsec.jar;%JAVA_HOME%\jre\lib\ext\localedata.jar;%JAVA_HOME%\jre\lib\ext\sunjce_provider.jar ocera.rtcan.monitor.CanMonitor %1 %2 %3 %4 %5 %6 %7 %8 %9 