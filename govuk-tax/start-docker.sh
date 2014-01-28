#!/bin/sh

exec /src/govuk-tax-1.0.1-SNAPSHOT/bin/govuk-tax \
  -Dhttp.port=8080 \
  -Dapplication.log=INFO \
  -Dlogger.resource=/govuk-tax-logger.xml \
  -Dconfig.resource=/application.conf \
  $HMRC_CONFIG
