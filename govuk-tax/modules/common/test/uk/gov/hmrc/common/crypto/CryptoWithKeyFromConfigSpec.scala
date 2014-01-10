package uk.gov.hmrc.common.crypto

import uk.gov.hmrc.common.BaseSpec
import play.api.test.{FakeApplication, WithApplication}
import org.apache.commons.codec.binary.Base64

class CryptoWithKeyFromConfigSpec extends BaseSpec {

  private val configKey = "crypto.spec.key"

  private val message = "this is my message"
  private val encryptedMessage = "up/76On5j54pAjzqZR1mqM5E28skTl8Aw0GkKi+zjkk="

  private val fakeApplicationWithValidKey = {
    val rawCryptoKey = Array[Byte](0, 1, 2, 3, 4, 5 ,6 ,7, 8 ,9, 10, 11, 12, 13, 14, 15)
    val cryptoKey = Base64.encodeBase64String(rawCryptoKey)
    FakeApplication(additionalConfiguration = Map(configKey -> cryptoKey))
  }

  private val fakeApplicationWithoutValidKey = FakeApplication()

  "Constructing a CryptoWithKeyFromConfig" should {

    "return a properly initialised SymmetricCrypto object" in new WithApplication(fakeApplicationWithValidKey)  {
      val crypto = CryptoWithKeyFromConfig(configKey)
      crypto.encrypt(message) shouldBe encryptedMessage
      crypto.decrypt(encryptedMessage) shouldBe message
    }

    "throw a SecurityException on first use of encrypt if the key is missing from the config" in new WithApplication(fakeApplicationWithoutValidKey) {
      val crypto = CryptoWithKeyFromConfig(configKey)
      val thrown = the [SecurityException] thrownBy crypto.encrypt(message)
      thrown.getMessage should include (configKey)
    }

    "throw a SecurityException on first use of decrypt if the key is missing from the config" in new WithApplication(fakeApplicationWithoutValidKey) {
      val crypto = CryptoWithKeyFromConfig(configKey)
      val thrown = the [SecurityException] thrownBy crypto.decrypt(encryptedMessage)
      thrown.getMessage should include (configKey)
    }
  }
}
