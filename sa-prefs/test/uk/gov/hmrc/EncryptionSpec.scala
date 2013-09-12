package uk.gov.hmrc

import org.scalatest.{ ShouldMatchers, WordSpec }
import org.joda.time.{ DateTimeZone, DateTime }
import uk.gov.hmrc.secure.SymmetricEncrypter
import org.apache.commons.codec.binary.Base64
import java.net.URLEncoder

class EncryptionSpec extends WordSpec with ShouldMatchers with TokenEncryption {

  override val encryptionKey = "P5xsJ9Nt+quxGZzB4DeLfw=="

  "Token decryption " should {
    "decrypt a valid token" in {
      val validToken = s"utr:${DateTime.now(DateTimeZone.UTC).getMillis}"
      val encryptedToken = encrypt(validToken)

      decryptToken(encryptedToken) shouldBe "utr"
    }

    "fail with expired token" in {
      val expiredToken = s"utr:${DateTime.now(DateTimeZone.UTC).minusMinutes(6).getMillis}"
      val encryptedToken = encrypt(expiredToken)

      intercept[TokenExpiredException] {
        decryptToken(encryptedToken)
      }
    }

    "fail with corrupted token" in {
      intercept[SecurityException] {
        decryptToken("invalid")
      }
    }
  }

}
