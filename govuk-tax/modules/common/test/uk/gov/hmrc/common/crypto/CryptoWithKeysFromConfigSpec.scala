package uk.gov.hmrc.common.crypto

import uk.gov.hmrc.common.BaseSpec
import play.api.test.{FakeApplication, WithApplication}
import org.apache.commons.codec.binary.Base64

class CryptoWithKeysFromConfigSpec extends BaseSpec {

  private val baseConfigKey = "crypto.spec"

  private object CurrentKey {
    val configKey = baseConfigKey + ".key"
    val encryptionKey = Base64.encodeBase64String(Array[Byte](0, 1, 2, 3, 4, 5 ,6 ,7, 8 ,9, 10, 11, 12, 13, 14, 15))
    val plainMessage = "this is my message"
    val encryptedMessage = "up/76On5j54pAjzqZR1mqM5E28skTl8Aw0GkKi+zjkk="
  }
  
  private object PreviousKey1 {
    val encryptionKey = Base64.encodeBase64String(Array[Byte](1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1))
    val plainMessage = "this is the first plain message"
    val encryptedMessage = "4WRjfZOzsem4vUW4LHTw2tyrHZ0ex8S9RQcyQeul868="
  }
  
  private object PreviousKey2 {
    val encryptionKey = Base64.encodeBase64String(Array[Byte](2, 2 ,2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2))
    val plainMessage = "this is the second plain message"
    val encryptedMessage = "PmNS+l5oVk2JNVyJfa0lANm9s2qy3uAlfEBl1n0AqoM6TyxjVl4j44MU4z7Bydih"
  }

  private object PreviousKeys {
    val configKey = baseConfigKey + ".previousKeys"
    val encryptionKeys = Seq(PreviousKey1.encryptionKey, PreviousKey2.encryptionKey)
  }

  "Constructing a CompositeCryptoWithKeysFromConfig without current or previous keys" should {

    val fakeApplicationWithoutAnyKeys = FakeApplication()

    "throw a SecurityException on construction" in new WithApplication(fakeApplicationWithoutAnyKeys) {
      evaluating(CryptoWithKeysFromConfig(baseConfigKey)) should produce [SecurityException]
    }
  }

  "Constructing a CompositeCryptoWithKeysFromConfig without a current key, but with previous keys" should {

    val fakeApplicationWithPreviousKeysOnly = FakeApplication(additionalConfiguration = Map(
      PreviousKeys.configKey -> PreviousKeys.encryptionKeys
    ))

    "throw a SecurityException on construction" in new WithApplication(fakeApplicationWithPreviousKeysOnly) {
      evaluating(CryptoWithKeysFromConfig(baseConfigKey)) should produce [SecurityException]
    }
  }

  "Constructing a CompositeCryptoWithKeysFromConfig with a current key, but no previous keys configured" should {

    val fakeApplicationWithCurrentKeyOnly = FakeApplication(additionalConfiguration = Map(
      CurrentKey.configKey -> CurrentKey.encryptionKey
    ))

    "return a properly initialised, functional CompositeSymmetricCrypto object" in new WithApplication(fakeApplicationWithCurrentKeyOnly)  {
      val crypto = CryptoWithKeysFromConfig(baseConfigKey)
      crypto.encrypt(CurrentKey.plainMessage) shouldBe CurrentKey.encryptedMessage
      crypto.decrypt(CurrentKey.encryptedMessage) shouldBe CurrentKey.plainMessage
    }
  }

  "Constructing a CompositeCryptoWithKeysFromConfig with a current key and empty previous keys" should {

    val fakeApplicationWithEmptyPreviousKeys = {
      FakeApplication(additionalConfiguration = Map(
        CurrentKey.configKey -> CurrentKey.encryptionKey,
        PreviousKeys.configKey -> List.empty))
    }

    "return a properly initialised, functional CompositeSymmetricCrypto object that works with the current key" in new WithApplication(fakeApplicationWithEmptyPreviousKeys)  {
      val crypto = CryptoWithKeysFromConfig(baseConfigKey)
      crypto.encrypt(CurrentKey.plainMessage) shouldBe CurrentKey.encryptedMessage
      crypto.decrypt(CurrentKey.encryptedMessage) shouldBe CurrentKey.plainMessage
    }
  }

  "Constructing a CompositeCryptoWithKeysFromConfig with both current and previous keys" should {

    val fakeApplicationWithCurrentAndPreviousKeys = {
      FakeApplication(additionalConfiguration = Map(
        CurrentKey.configKey -> CurrentKey.encryptionKey,
        PreviousKeys.configKey -> PreviousKeys.encryptionKeys))
    }

    "return a properly initialised, functional CompositeSymmetricCrypto object that works with both old and new keys" in new WithApplication(fakeApplicationWithCurrentAndPreviousKeys)  {
      val crypto = CryptoWithKeysFromConfig(baseConfigKey)
      crypto.encrypt(CurrentKey.plainMessage) shouldBe CurrentKey.encryptedMessage
      crypto.decrypt(CurrentKey.encryptedMessage) shouldBe CurrentKey.plainMessage
      crypto.decrypt(PreviousKey1.encryptedMessage) shouldBe PreviousKey1.plainMessage
      crypto.decrypt(PreviousKey2.encryptedMessage) shouldBe PreviousKey2.plainMessage
    }
  }
}
