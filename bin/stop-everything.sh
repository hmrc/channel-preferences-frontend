#!/bin/bash

DIR="$( cd "$( dirname "$0" )/.." && pwd )"
cd ${DIR}/govuk-tax 

if [ -f RUNNING_PID ]
then
	echo "Stopping frontend server [pid: $( cat RUNNING_PID )]"
	kill "$( cat RUNNING_PID )"
	echo "Server stopped"
else
	echo "Frontend server is not running."
fi

cd ${DIR}
