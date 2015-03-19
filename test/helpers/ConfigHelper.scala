package helpers

import play.api.test.FakeApplication

object ConfigHelper {

  def additionalConfig = Map(
    "govuk-tax.Test.services.contact-frontend.host" -> "localhost",
    "govuk-tax.Test.services.contact-frontend.port" -> "9250")

  def fakeApp = FakeApplication(additionalConfiguration = additionalConfig)

}
