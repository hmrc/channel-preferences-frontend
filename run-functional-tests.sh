#!/bin/bash
sm --start ASSETS_FRONTEND -fo
sbt -Dbrowser=chrome -Dwebdriver.chrome.driver=/usr/local/bin/chromedriver functional:test
sm --stop ASSETS_FRONTEND
