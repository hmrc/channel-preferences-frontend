package uk.gov.hmrc.common.crypto

import uk.gov.hmrc.common.BaseSpec
import play.api.test.{FakeApplication, WithApplication}
import SymmetricCryptoTestData.{plainMessage, encryptedMessage}
import org.apache.commons.codec.binary.Base64

class CompositeCryptoWithKeysFromConfigSpec extends BaseSpec {

  private object PreviousSymmetricCryptoTestData {

    val previousKey1 = Base64.encodeBase64String(Array[Byte](1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1))
    val previousKey2 = Base64.encodeBase64String(Array[Byte](2, 2 ,2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2))

    val encryptionKeys = Seq(previousKey1, previousKey2)

    val plainMessage1 = "this is the first plain message"
    val message1EncryptedWithPreviousKey1 = "4WRjfZOzsem4vUW4LHTw2tyrHZ0ex8S9RQcyQeul868="

    val plainMessage2 = "this is the second plain message"
    val message2EncryptedWithPreviousKey2 = "PmNS+l5oVk2JNVyJfa0lANm9s2qy3uAlfEBl1n0AqoM6TyxjVl4j44MU4z7Bydih"
  }

  private val baseConfigKey = "crypto.spec.key"
  
  private val currentConfigKey = baseConfigKey + ".current"
  private val currentEncryptionKey = SymmetricCryptoTestData.encryptionKey

  private val previousConfigKey = baseConfigKey + ".previous"
  private val previousEncryptionKeys = PreviousSymmetricCryptoTestData.encryptionKeys

  "Constructing a CompositeCryptoWithKeysFromConfig without current or previous keys" should {

    val fakeApplicationWithoutAnyKeys = FakeApplication()

    "throw a SecurityException on first use of encrypt" in new WithApplication(fakeApplicationWithoutAnyKeys) {
      val crypto = CompositeCryptoWithKeysFromConfig(baseConfigKey)
      evaluating(crypto.encrypt(plainMessage)) should produce [SecurityException]
    }

    "throw a SecurityException on first use of decrypt" in new WithApplication(fakeApplicationWithoutAnyKeys) {
      val crypto = CompositeCryptoWithKeysFromConfig(baseConfigKey)
      evaluating(crypto.decrypt(encryptedMessage)) should produce [SecurityException]
    }
  }

  "Constructing a CompositeCryptoWithKeysFromConfig without a current key, but with previous keys" should {

    val fakeApplicationWithPreviousKeysOnly = FakeApplication(additionalConfiguration = Map(
      previousConfigKey -> previousEncryptionKeys
    ))

    "throw a SecurityException on first use of encrypt" in new WithApplication(fakeApplicationWithPreviousKeysOnly) {
      val crypto = CompositeCryptoWithKeysFromConfig(baseConfigKey)
      evaluating(crypto.encrypt(plainMessage)) should produce [SecurityException]
    }

    "throw a SecurityException on first use of decrypt" in new WithApplication(fakeApplicationWithPreviousKeysOnly) {
      val crypto = CompositeCryptoWithKeysFromConfig(baseConfigKey)
      evaluating(crypto.decrypt(encryptedMessage)) should produce [SecurityException]
    }
  }

  "Constructing a CompositeCryptoWithKeysFromConfig with a current key, but no previous keys configured" should {

    val fakeApplicationWithCurrentKeyOnly = FakeApplication(additionalConfiguration = Map(
      currentConfigKey -> currentEncryptionKey
    ))

    // TODO Due to lazy instantiation, this currently doesn't throw an exception.  Review setup of configuration.
    "throw a SecurityException on first use of encrypt" ignore new WithApplication(fakeApplicationWithCurrentKeyOnly) {
      val crypto = CompositeCryptoWithKeysFromConfig(baseConfigKey)
      evaluating(crypto.encrypt(plainMessage)) should produce [SecurityException]
    }

    "throw a SecurityException on first use of decrypt" in new WithApplication(fakeApplicationWithCurrentKeyOnly) {
      val crypto = CompositeCryptoWithKeysFromConfig(baseConfigKey)
      evaluating(crypto.decrypt(encryptedMessage)) should produce [SecurityException]
    }
  }

  "Constructing a CompositeCryptoWithKeysFromConfig with a current key and empty previous keys" should {

    val fakeApplicationWithEmptyPreviousKeys = {
      FakeApplication(additionalConfiguration = Map(
        currentConfigKey -> currentEncryptionKey,
        previousConfigKey -> List.empty))
    }

    "return a properly initialised, functional CompositeSymmetricCrypto object" in new WithApplication(fakeApplicationWithEmptyPreviousKeys)  {
      val crypto = CompositeCryptoWithKeysFromConfig(baseConfigKey)
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

    "return a properly initialised, functional CompositeSymmetricCrypto object" in new WithApplication(fakeApplicationWithCurrentAndPreviousKeys)  {
      val crypto = CompositeCryptoWithKeysFromConfig(baseConfigKey)
      crypto.encrypt(plainMessage) shouldBe encryptedMessage
      crypto.decrypt(encryptedMessage) shouldBe plainMessage
      crypto.decrypt(message1EncryptedWithPreviousKey1) shouldBe plainMessage1
      crypto.decrypt(message2EncryptedWithPreviousKey2) shouldBe plainMessage2
    }
  }
}
