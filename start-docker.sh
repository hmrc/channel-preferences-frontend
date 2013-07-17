#!/bin/sh
echo "127.0.0.1 $(hostname)" > /etc/hosts
exec sh -ex /src/govuk-tax-0.0.1-SNAPSHOT/start -Dhttp.port=8080
