package uk.gov.hmrc.common.crypto

import uk.gov.hmrc.common.BaseSpec
import org.apache.commons.codec.binary.Base64

class SymmetricCryptoSpec extends BaseSpec {

  trait Setup {

    private val rawKey = Array[Byte](0, 1, 2, 3, 4, 5 ,6 ,7, 8 ,9, 10, 11, 12, 13, 14, 15)
    private val encodedKey = Base64.encodeBase64String(rawKey)

    val crypto = new SymmetricCrypto {
      override val encryptionKey = encodedKey
    }
  }

  "Encrypting a value" should {
    "UTF-8 encode the value, then AES encrypt it using the key and base-64 the result" in new Setup {
      crypto.encrypt("this is my message") shouldBe "up/76On5j54pAjzqZR1mqM5E28skTl8Aw0GkKi+zjkk="
    }
  }

  "Decrypting a value" should {
    "take the encrypted, base-64 encoded string, and return the original message" in new Setup {
      crypto.decrypt("up/76On5j54pAjzqZR1mqM5E28skTl8Aw0GkKi+zjkk=") shouldBe "this is my message"
    }
  }
}
