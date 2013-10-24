#!/bin/sh
#echo "127.0.0.1 $(hostname)" > /etc/hosts
exec /src/sa-prefs-0.0.1-SNAPSHOT/bin/sa-prefs -Dhttp.port=8080 -Dapplication.log=INFO -Dlogger.resource=/sa-prefs-logger.xml -Dconfig.resource=/preview.conf
