#!/bin/bash

DBHOST=localhost
USER=root
PASS=
DBNAME=l2gw

while :;
do
        mv log/java0.log.0 "log/`date +%Y-%m-%d_%H-%M-%S`_java.log"
        mv log/stdout.log "log/`date +%Y-%m-%d_%H-%M-%S`_stdout.log"
        CP=""
        for i in `ls *.jar`
        do
            CP=${CP}":"$i
        done
        nice -n -2 java -Dfile.encoding=UTF-8 -Xincgc -Xms256m -Xmx256m -cp $CP ru.l2gw.fakeserver.FakeServer > log/stdout.log 2>&1
        [ $? -ne 2 ] && break
        sleep 10;
done
