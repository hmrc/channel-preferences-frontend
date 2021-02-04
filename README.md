
# preferences-frontend

## Run the project locally 

`sbt run "9053 -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes"`

## Run the tests and sbt fmt before raising a PR

Ensure you have service-manager python environment setup:

`source ../servicemanager/bin/activate`

Format:

`sbt fmt`

Then run the tests and coverage report:

`sbt clean coverage test coverageReport`

If your build fails due to poor testing coverage, *DO NOT* lower the test coverage, instead inspect the generated report located here on your local repo: `/target/scala-2.12/scoverage-report/index.html`

Then run the integration tests:

`sbt it:test`

## Swagger endpoint

Available locally here: http://localhost:9053/preferences-frontend/api/schema.json

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
