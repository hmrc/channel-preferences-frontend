#!/bin/sh

cd govuk-tax
./sbt test

cd ../sa-prefs
./sbt test
