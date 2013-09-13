#!/bin/bash

if [ $# > 1 ]; then
        conf_file=$1
fi

. ./build-env

cd govuk-tax

./sbt clean test dist

cd ../sa-prefs

./sbt clean test dist

sed -ibak "s/CONF_FILE/$conf_file/g" ../start-docker.sh
