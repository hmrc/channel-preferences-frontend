package uk.gov.hmrc.common.crypto

import uk.gov.hmrc.common.BaseSpec
import play.api.test.{FakeApplication, WithApplication}

class CryptoWithKeyFromConfigSpec extends BaseSpec {

  "Constructing a CryptoWithKeyFromConfig" should {

    import SymmetricCryptoTestData._

    val configKey = "crypto.spec.key"

    val fakeApplicationWithValidKey = {
      FakeApplication(additionalConfiguration = Map(configKey -> encryptionKey))
    }

    val fakeApplicationWithoutValidKey = FakeApplication()

    "return a properly initialised SymmetricCrypto object" in new WithApplication(fakeApplicationWithValidKey)  {
      val crypto = CryptoWithKeyFromConfig(configKey)
      crypto.encrypt(plainMessage) shouldBe encryptedMessage
      crypto.decrypt(encryptedMessage) shouldBe plainMessage
    }

    "throw a SecurityException on first use of encrypt if the key is missing from the config" in new WithApplication(fakeApplicationWithoutValidKey) {
      val crypto = CryptoWithKeyFromConfig(configKey)
      val thrown = the [SecurityException] thrownBy crypto.encrypt(plainMessage)
      thrown.getMessage should include (configKey)
    }

    "throw a SecurityException on first use of decrypt if the key is missing from the config" in new WithApplication(fakeApplicationWithoutValidKey) {
      val crypto = CryptoWithKeyFromConfig(configKey)
      val thrown = the [SecurityException] thrownBy crypto.decrypt(encryptedMessage)
      thrown.getMessage should include (configKey)
    }
  }
}
