#!/bin/sh
#echo "127.0.0.1 $(hostname)" > /etc/hosts
exec sh -ex /src/govuk-tax-0.0.1-SNAPSHOT/start -Dhttp.port=8080 -Dapplication.log=INFO -Dlogger.resource=/govuk-tax-logger.xml -Dconfig.resource=/qa-stubida.conf -Dgovuk-tax.Prod.services.saml.host=hod.service
