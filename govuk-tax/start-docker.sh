#!/bin/sh

SCRIPT=$(find . -type f -name govuk-tax)
exec $SCRIPT \
  -Dhttp.port=8080 \
  -Dapplication.log=INFO \
  -Dlogger.resource=/govuk-tax-logger.xml \
  -Dconfig.resource=/application.conf \
  $HMRC_CONFIG
