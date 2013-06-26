#!/bin/bash

DIR="$( cd "$( dirname "$0" )/.." && pwd )"
cd ${DIR}/govuk-tax 
./sbt stage
./target/start & &> ./logs/govuk-tax-frontend.out
cd ${DIR} 
