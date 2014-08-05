#!/bin/bash

java -classpath ".:bin/tcp-ping.jar:lib/*" -Dlog4j.configurationFile=conf/log4j2.xml org.ivica.demo.tcpping.TCPPing -p -port 55555 -size 300 -mps 5 127.0.0.1