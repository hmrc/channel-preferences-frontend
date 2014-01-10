package controllers.common

import play.api.test.{ FakeApplication, WithApplication }
import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.common.crypto.SessionCookieCrypto

class SessionCookieCryptoSpec extends BaseSpec {

  "Cookie encryption and decryption" should {
    "return the original value" in new WithApplication(FakeApplication()) {
      val originalId = "/auth/oid/039470394602948620986029860298462"

      val encrypted = SessionCookieCrypto.encrypt(originalId)
      encrypted should not equal originalId

      val decryptedId = SessionCookieCrypto.decrypt(encrypted)
      decryptedId should equal(originalId)
    }
  }

}
