# Channel Preferences Frontend Contributor Guidelines

Hello! Thank you for taking the time to contribute to [channel-preferences-frontend](https://github.com/hmrc/channel-preferences-frontend).

## General Contributor Guidelines

Before you go any further, please read the general [MDTP Contributor Guidelines](https://github.com/hmrc/mdtp-contributor-guidelines/blob/master/CONTRIBUTING.md).
It would be helpful if you were to talk to the team before making a pull request to stop any conflicts that may occur.

## Channel Preferences Frontend (CPF) Guidelines

CPF sbt build is devided into 3 sub-projects.
- root sub-project
- legacy sub-project - a migration from the preferences-frontend microservice
- cpf sub-project - a place for new and migrated functionality

Please develop new and migrated functionality in cpf.

To migrate an endpoint remove it from legacy/conf/preference-frontend.routes and add to cpf/conf/cpf.routes.
