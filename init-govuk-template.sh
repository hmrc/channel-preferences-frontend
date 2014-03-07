#!/bin/sh
echo "Adding govuk_template_play submodule.."
git submodule add git@github.com:alphagov/govuk_template_play.git govuk-tax/modules/common/public/govuk_template_play
git submodule init

echo "Updating govuk_template_play"

git submodule update
