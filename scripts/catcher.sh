#!/bin/bash

java -classpath ".:bin/tcp-ping.jar:lib/*" -Dlog4j.configurationFile=conf/log4j2.xml org.ivica.demo.tcpping.TCPPing -c -port 55555 -bind 127.0.0.1