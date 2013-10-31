package controllers.bt.testframework.request

import controllers.common.CookieEncryption
import java.util.UUID
import play.api.test.FakeRequest
import org.joda.time.{DateTimeZone, DateTime}
import controllers.bt.testframework.mocks.DateTimeProviderMock

trait EmptySessionRequest extends CookieEncryption {

  def request = {
    val session = ("sessionId", encrypt(s"session-${UUID.randomUUID().toString}"))
    FakeRequest().withSession(session)
  }

}