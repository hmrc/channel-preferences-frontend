/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.filing

import org.joda.time.{ DateTime, DateTimeZone }
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.crypto.{ Crypted, PlainText }
import uk.gov.hmrc.domain.SaUtr

import java.net.{ URLDecoder, URLEncoder }

class TokenEncryptionSpec extends PlaySpec with GuiceOneAppPerSuite {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "sso.encryption.key"          -> "P5xsJ9Nt+quxGZzB4DeLfw==",
        "sso.encryption.previousKeys" -> Seq.empty
      )
      .build()
  val crypto = app.injector.instanceOf[TokenEncryption]
  "Token decryption" should {
    "decrypt a valid token" in {
      val validToken = s"utr:${DateTime.now(DateTimeZone.UTC).getMillis}"
      val encryptedToken = URLEncoder.encode(crypto.encrypt(PlainText(validToken)).value, "UTF-8")

      crypto.decryptToken(encryptedToken, 5) must have('utr (SaUtr("utr")))
    }

    "decrypt a valid unencoded token" in {
      val validToken = s"cjsajjdajdas:${DateTime.now(DateTimeZone.UTC).getMillis}"
      val encryptedToken = crypto.encrypt(PlainText(validToken))

      crypto.decryptToken(encryptedToken.value, 5) must have('utr (SaUtr("cjsajjdajdas")))
    }

    "decrypt token with slashes and plus chars" in {
      val encoded = "vK%2Bps%2FoV3CYFc0fgzd1ZiBIUu%2FQ%2FVmAeDNcUkgRs%2BTE%3D"
      val token = URLDecoder.decode(encoded, "UTF-8")
      crypto.decrypt(Crypted(token)).value mustBe "cjsajjdajdas:1379068252455"
    }

    "fail with expired token" in {
      val expiredToken = s"utr:${DateTime.now(DateTimeZone.UTC).minusMinutes(6).getMillis}"
      val encryptedToken = URLEncoder.encode(crypto.encrypt(PlainText(expiredToken)).value, "UTF-8")

      intercept[TokenExpiredException] {
        crypto.decryptToken(encryptedToken, 5)
      }
    }

    "fail with corrupted token" in {
      intercept[SecurityException] {
        crypto.decryptToken("invalid", 5)
      }
    }
  }

}
