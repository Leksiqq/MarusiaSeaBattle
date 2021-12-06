@echo off
set url=https://leksi.net/sea-battle-dev/marusia/speaker
set with_error=true
set with_fail=true
set num_games=2
set num_rules=3
set with_help=true
set players=ME

set FILE=./1.txt
set FROM=edipro@translog.org
set TO=leksi@leksi.net
set SUBJ=Marusia testing
set TOMCAL_LIB=C:\Program Files\Apache Software Foundation\Tomcat 10.0\lib
set GENERIC=F:\leksi\Java\games\2-part-paper\basic_proc\out\artifacts\basic-proc.jar
set MAIL_SENDER=F:\leksi\Java\games\2-part-paper\mail_sender\out\artifacts\mail-sender.jar
set PROC=F:\leksi\Java\games\2-part-paper\sea-battle\proc\out\artifacts\sea-battle-proc.jar
set M2_REPOS=C:\Users\alexei\.m2\repository
set MAIL_SENDER=%M2_REPOS%\javax\mail\javax.mail-api\1.6.2\javax.mail-api-1.6.2.jar;%M2_REPOS%\com\sun\mail\javax.mail\1.6.1\javax.mail-1.6.1.jar;%M2_REPOS%\javax\activation\activation\1.1\activation-1.1.jar
"java.exe" -Dwith_error=%with_error% -Dwith_fail=%with_fail% -Dwith_help=%with_help% -Dnum_games=%num_games% -Dnum_rules=%num_rules% -Dplayers=%players% -Dfile.encoding=UTF-8 -classpath "%GENERIC%;%M2_REPOS%\com\googlecode\json-simple\json-simple\1.1.1\json-simple-1.1.1.jar;%PROC%" net.leksi.sea_battle.WebHookTester %url% > "%FILE%" 2>&1
rem "java.exe" -Dfile.encoding=UTF-8 -Dmail.smtp.host=mail.office.lan -Dmail.smtp.port=25 -DFROM=%FROM% -DTO=%TO% -classpath "%GENERIC%;%MAIL_SENDER%" net.leksi.games.basic.MailSender "%FILE%" "%SUBJ%"
rem del "%FILE%"

