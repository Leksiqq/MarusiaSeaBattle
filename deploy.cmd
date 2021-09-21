set APP=sea-battle
set TARGET=C:\Program Files\Apache Software Foundation\Tomcat 10.0\webapps\%APP%
del /F /Q "%TARGET%"
FOR /D %%p IN ("%TARGET%\*") DO rmdir "%%p" /s /q
xcopy web\* /E /Y "C:\Program Files\Apache Software Foundation\Tomcat 10.0\webapps\%APP%"
touch "C:\Program Files\Apache Software Foundation\Tomcat 10.0\webapps\%APP%\WEB-INF\web.xml"

