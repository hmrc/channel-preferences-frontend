#!/bin/bash

if [ -f "/etc/buildrc" ]; then
  . /etc/buildrc
else
  . ./build-env
fi

scriptname=$(basename $0)
echo "[${scriptname}] Using ivy directory ${HOME}/.ivy2"

export SA_PREFS_VERSION=`git describe --abbrev=0 --match release/* | sed s/release\\\///`

./sbt -Dsbt.log.noformat=true clean dist publish
