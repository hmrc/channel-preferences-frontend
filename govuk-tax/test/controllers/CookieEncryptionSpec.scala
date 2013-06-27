package controllers

import test.BaseSpec
import play.api.test.{ FakeApplication, WithApplication }

class CookieEncryptionSpec extends BaseSpec with CookieEncryption {

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
