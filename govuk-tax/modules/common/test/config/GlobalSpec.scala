package config

import uk.gov.hmrc.common.BaseSpec
import play.api.test.{WithApplication, FakeApplication}

class GlobalSpec extends BaseSpec {

  private val invalidKey = "ZGVmZ2hpamtsbW4K"
  private val validKey = "MTIzNDU2Nzg5MDEyMzQ1Cg=="

  private val mapWithValidKeys = Map(
    "cookie.encryption.key" -> validKey,
    "cookie.encryption.previousKeys" -> Seq.empty,
    "sso.encryption.key" -> validKey,
    "sso.encryption.previousKeys" -> Seq.empty,
    "queryParameter.encryption.key" -> validKey,
    "queryParameter.encryption.previousKeys" -> Seq.empty
  )

  private def startup(app: FakeApplication): Boolean = {
    new WithApplication(app) {
      app.global shouldBe Global // Force instantiation of the Global object - which lazily kicks off the app startup.
    }
    true
  }

  "The onStart method" should {

    "Complete without any problems if all encryption keys are valid" in {
      val appWithValidEncryptionKeys = FakeApplication(additionalConfiguration = mapWithValidKeys)
      startup(appWithValidEncryptionKeys) shouldBe true
    }

    "Propagate exceptions thrown due to an invalid cookie encryption key" in {
      val appWithInvalidCookieEncryptionKey = FakeApplication(additionalConfiguration = mapWithValidKeys + ("cookie.encryption.key" -> invalidKey))
      evaluating(startup(appWithInvalidCookieEncryptionKey)) should produce[SecurityException]
    }

    "Propagate exceptions thrown due to an invalid sso encryption key" in {
      val appWithInvalidSsoEncryptionKey = FakeApplication(additionalConfiguration = mapWithValidKeys + ("sso.encryption.key" -> invalidKey))
      evaluating(startup(appWithInvalidSsoEncryptionKey)) should produce[SecurityException]
    }

    "Propagate exceptions thrown due to an invalid query parameter encryption key" in {
      val appWithInvalidQueryParameterEncryptionKey = FakeApplication(additionalConfiguration = mapWithValidKeys + ("queryParameter.encryption.key" -> invalidKey))
      evaluating(startup(appWithInvalidQueryParameterEncryptionKey)) should produce[SecurityException]
    }
  }
}
