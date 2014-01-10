package uk.gov.hmrc

import org.scalatest.{ ShouldMatchers, WordSpec }
import org.joda.time.{ DateTimeZone, DateTime }
import uk.gov.hmrc.secure.SymmetricEncrypter
import org.apache.commons.codec.binary.Base64
import java.net.{ URLDecoder, URLEncoder }

class EncryptionSpec extends WordSpec with ShouldMatchers with TokenEncryption {

  override val encryptionKey = "P5xsJ9Nt+quxGZzB4DeLfw=="

  "Token decryption" should {
    "decrypt a valid token" in {
      val validToken = s"utr:${DateTime.now(DateTimeZone.UTC).getMillis}"
      val encryptedToken = URLEncoder.encode(encrypt(validToken), "UTF-8")

      decryptToken(encryptedToken, 5) shouldBe "utr"
    }

    "decrypt a valid unencoded token" in {
      val validToken = s"cjsajjdajdas:${DateTime.now(DateTimeZone.UTC).getMillis}"
      val encryptedToken = encrypt(validToken)

      decryptToken(encryptedToken, 5) shouldBe "cjsajjdajdas"
    }

    "decrypt token with slashes and plus chars" in {
      val encoded = "vK%2Bps%2FoV3CYFc0fgzd1ZiBIUu%2FQ%2FVmAeDNcUkgRs%2BTE%3D"
      val token = URLDecoder.decode(encoded, "UTF-8")
      decrypt(token) shouldBe "cjsajjdajdas:1379068252455"
    }

    "fail with expired token" in {
      val expiredToken = s"utr:${DateTime.now(DateTimeZone.UTC).minusMinutes(6).getMillis}"
      val encryptedToken = URLEncoder.encode(encrypt(expiredToken), "UTF-8")

      intercept[TokenExpiredException] {
        decryptToken(encryptedToken, 5)
      }
    }

    "fail with corrupted token" in {
      intercept[SecurityException] {
        decryptToken("invalid", 5)
      }
    }
  }

}

