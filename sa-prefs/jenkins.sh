#!/bin/bash

if [ $# > 1 ]; then
        conf_file=$1
fi

. ./build-env

cd sa-prefs

./sbt clean test dist publish
