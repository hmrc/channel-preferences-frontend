#!/bin/sh

SCRIPT=$(find . -type f -name sa-prefs)
exec $SCRIPT \
  $HMRC_CONFIG
