package controllers

import play.api.test.{ FakeApplication, WithApplication }
import uk.gov.hmrc.common.BaseSpec
import controllers.common._

class CookieCryptoSpec extends BaseSpec with CookieCrypto {

  "Cookie encryption and decryption" should {
    "return the original value" in new WithApplication(FakeApplication()) {
      val originalId = "/auth/oid/039470394602948620986029860298462"

      val encrypted = encrypt(originalId)
      encrypted should not equal (originalId)

      val decryptedId = decrypt(encrypted)
      decryptedId should equal(originalId)
    }
  }

}
