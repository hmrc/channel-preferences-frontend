#!/bin/sh

exec /src/sa-prefs-0.0.1-SNAPSHOT/bin/sa-prefs \
  -Dhttp.port=8080 \
  -Dapplication.log=INFO \
  -Dlogger.resource=/sa-prefs-logger.xml \
  -Dconfig.resource=/application.conf \
  $HMRC_CONFIG

