
# channel-preferences-frontend

## Run the project locally 

`sbt run "9053 -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes"`

## Run the tests and sbt fmt before raising a PR

Ensure you have service-manager python environment setup:

`source ../servicemanager/bin/activate`

Format:

`sbt fmt`

Then run the tests:

`sbt test it:test`

## Swagger endpoint

Available locally here: http://localhost:9053/channel-preferences-frontend/api/schema.json

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").


