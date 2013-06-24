#!/bin/bash

. ./build-env

cd govuk-tax
play clean test dist
