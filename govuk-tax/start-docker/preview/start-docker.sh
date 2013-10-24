#!/bin/sh
#echo "127.0.0.1 $(hostname)" > /etc/hosts
exec /src/govuk-tax-0.0.1-SNAPSHOT/bin/govuk-tax -Dhttp.port=8080 -Dapplication.log=INFO -Dlogger.resource=/govuk-tax-logger.xml -Dconfig.resource=/preview.conf
