package helpers

import play.api.test.FakeApplication

object ConfigHelper {

  def additionalConfig = Map(
    "govuk-tax.Test.services.contact-frontend.host" -> "localhost",
    "govuk-tax.Test.services.contact-frontend.port" -> "9250",
    "govuk-tax.Test.assets.url" -> "fake/url",
    "govuk-tax.Test.assets.version" -> "54321",
    "govuk-tax.Test.google-analytics.host" -> "host",
    "govuk-tax.Test.google-analytics.token" -> "aToken")

  def fakeApp = FakeApplication(additionalConfiguration = additionalConfig)

}
