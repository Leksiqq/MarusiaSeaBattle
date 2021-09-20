set APP=sea-battle-dev
xcopy web\* /E /Y "C:\Program Files\Apache Software Foundation\Tomcat 10.0\webapps\%APP%"
touch "C:\Program Files\Apache Software Foundation\Tomcat 10.0\webapps\%APP%\WEB-INF\web.xml"

