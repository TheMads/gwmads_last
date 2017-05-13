#!/bin/bash

DBHOST=localhost
USER=root
PASS=
DBNAME=l2gw

while :;
do
        #mysqlcheck -h $DBHOST -u $USER --password=$PASS -s -r $DBNAME>>"log/`date +%Y-%m-%d_%H:%M:%S`-sql_check.log"
        #mysqldump -h $DBHOST -u $USER --password=$PASS $DBNAME|zip "backup/`date +%Y-%m-%d_%H:%M:%S`-l2fdb_loginserver.zip" -
        mv log/java0.log.0 "log/`date +%Y-%m-%d_%H-%M-%S`_java.log"
        mv log/stdout.log "log/`date +%Y-%m-%d_%H-%M-%S`_stdout.log"
        nice -n -2 java -server -Xms64m -Xmx65m -cp l2login.jar ru.l2gw.loginserver.L2LoginServer > log/stdout.log 2>&1
        [ $? -ne 2 ] && break
        sleep 10;
done
