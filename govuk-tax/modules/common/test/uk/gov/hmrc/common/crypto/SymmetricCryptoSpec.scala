package uk.gov.hmrc.common.crypto

import uk.gov.hmrc.common.BaseSpec
import SymmetricCryptoTestData.{plainMessage, encryptedMessage}

class SymmetricCryptoSpec extends BaseSpec {

  trait Setup {
    val crypto = new SymmetricCrypto {
      override val encryptionKey = SymmetricCryptoTestData.key
    }
  }

  "Encrypting a value" should {

    "UTF-8 encode the value, then AES encrypt it using the key and base-64 the result" in new Setup {
      crypto.encrypt(plainMessage) shouldBe encryptedMessage
    }
  }

  "Decrypting a value" should {
    "take the encrypted, base-64 encoded string, and return the original message" in new Setup {
      crypto.decrypt(encryptedMessage) shouldBe plainMessage
    }
  }
}
