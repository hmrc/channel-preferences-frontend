#!/bin/bash
sm --start ASSETS_FRONTEND -f
sbt functional:test
sm --stop ASSETS_FRONTEND
