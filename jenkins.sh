#!/bin/bash

. ./build-env

cd govuk-tax

./sbt clean test dist

