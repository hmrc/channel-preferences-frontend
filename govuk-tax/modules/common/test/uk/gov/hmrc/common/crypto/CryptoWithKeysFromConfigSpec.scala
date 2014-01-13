package uk.gov.hmrc.common.crypto

import uk.gov.hmrc.common.BaseSpec
import play.api.test.{FakeApplication, WithApplication}
import SymmetricCryptoTestData.{plainMessage, encryptedMessage}
import org.apache.commons.codec.binary.Base64

class CryptoWithKeysFromConfigSpec extends BaseSpec {

  private object PreviousSymmetricCryptoTestData {

    val previousKey1 = Base64.encodeBase64String(Array[Byte](1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1))
    val previousKey2 = Base64.encodeBase64String(Array[Byte](2, 2 ,2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2))

    val encryptionKeys = Seq(previousKey1, previousKey2)

    val plainMessage1 = "this is the first plain message"
    val message1EncryptedWithPreviousKey1 = "4WRjfZOzsem4vUW4LHTw2tyrHZ0ex8S9RQcyQeul868="

    val plainMessage2 = "this is the second plain message"
    val message2EncryptedWithPreviousKey2 = "PmNS+l5oVk2JNVyJfa0lANm9s2qy3uAlfEBl1n0AqoM6TyxjVl4j44MU4z7Bydih"
  }

  private val baseConfigKey = "crypto.spec"
  
  private val currentConfigKey = baseConfigKey + ".key"
  private val currentEncryptionKey = SymmetricCryptoTestData.encryptionKey

  private val previousConfigKey = baseConfigKey + ".previousKeys"
  private val previousEncryptionKeys = PreviousSymmetricCryptoTestData.encryptionKeys

  "Constructing a CompositeCryptoWithKeysFromConfig without current or previous keys" should {

    val fakeApplicationWithoutAnyKeys = FakeApplication()

    "throw a SecurityException on construction" in new WithApplication(fakeApplicationWithoutAnyKeys) {
      evaluating(CryptoWithKeysFromConfig(baseConfigKey)) should produce [SecurityException]
    }
  }

  "Constructing a CompositeCryptoWithKeysFromConfig without a current key, but with previous keys" should {

    val fakeApplicationWithPreviousKeysOnly = FakeApplication(additionalConfiguration = Map(
      previousConfigKey -> previousEncryptionKeys
    ))

    "throw a SecurityException on construction" in new WithApplication(fakeApplicationWithPreviousKeysOnly) {
      evaluating(CryptoWithKeysFromConfig(baseConfigKey)) should produce [SecurityException]
    }
  }

  "Constructing a CompositeCryptoWithKeysFromConfig with a current key, but no previous keys configured" should {

    val fakeApplicationWithCurrentKeyOnly = FakeApplication(additionalConfiguration = Map(
      currentConfigKey -> currentEncryptionKey
    ))

    "return a properly initialised, functional CompositeSymmetricCrypto object" in new WithApplication(fakeApplicationWithCurrentKeyOnly)  {
      val crypto = CryptoWithKeysFromConfig(baseConfigKey)
      crypto.encrypt(plainMessage) shouldBe encryptedMessage
      crypto.decrypt(encryptedMessage) shouldBe plainMessage
    }
  }

  "Constructing a CompositeCryptoWithKeysFromConfig with a current key and empty previous keys" should {

    val fakeApplicationWithEmptyPreviousKeys = {
      FakeApplication(additionalConfiguration = Map(
        currentConfigKey -> currentEncryptionKey,
        previousConfigKey -> List.empty))
    }

    "return a properly initialised, functional CompositeSymmetricCrypto object that works with the current key" in new WithApplication(fakeApplicationWithEmptyPreviousKeys)  {
      val crypto = CryptoWithKeysFromConfig(baseConfigKey)
      crypto.encrypt(plainMessage) shouldBe encryptedMessage
      crypto.decrypt(encryptedMessage) shouldBe plainMessage
    }
  }

  "Constructing a CompositeCryptoWithKeysFromConfig with both current and previous keys" should {

    import PreviousSymmetricCryptoTestData._

    val fakeApplicationWithCurrentAndPreviousKeys = {
      FakeApplication(additionalConfiguration = Map(
        currentConfigKey -> currentEncryptionKey,
        previousConfigKey -> previousEncryptionKeys))
    }

    "return a properly initialised, functional CompositeSymmetricCrypto object that works with both old and new keys" in new WithApplication(fakeApplicationWithCurrentAndPreviousKeys)  {
      val crypto = CryptoWithKeysFromConfig(baseConfigKey)
      crypto.encrypt(plainMessage) shouldBe encryptedMessage
      crypto.decrypt(encryptedMessage) shouldBe plainMessage
      crypto.decrypt(message1EncryptedWithPreviousKey1) shouldBe plainMessage1
      crypto.decrypt(message2EncryptedWithPreviousKey2) shouldBe plainMessage2
    }
  }
}
