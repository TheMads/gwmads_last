#!/bin/bash

DBHOST=localhost
USER=root
PASS=
DBNAME=l2gw

while :;
do
        #mysqlcheck -h $DBHOST -u $USER --password=$PASS -s -r $DBNAME>>"log/`date +%Y-%m-%d_%H:%M:%S`-sql_check.log"
        #mysqldump -h $DBHOST -u $USER --password=$PASS $DBNAME|zip "backup/`date +%Y-%m-%d_%H:%M:%S`-l2fdb_gameserver.zip" -
        mv log/java0.log.0 "log/`date +%Y-%m-%d_%H-%M-%S`_java.log"
        mv log/stdout.log "log/`date +%Y-%m-%d_%H-%M-%S`_stdout.log"
        nice -n -2 java -Dfile.encoding=UTF-8 -Xincgc -Xms3072m -Xmx3072m -cp javolution.jar:c3p0-0.9.1.2.jar:mysql-connector-java-5.1.14-bin.jar:tools.jar:l2server.jar ru.l2gw.gameserver.GameServer > log/stdout.log 2>&1
        [ $? -ne 2 ] && break
        sleep 10;
done

