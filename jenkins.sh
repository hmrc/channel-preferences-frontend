#!/bin/bash

cd govuk-tax
./sbt -Dsbt.log.noformat=true clean test
