package controllers.bt.testframework.request

import controllers.common.CookieCrypto
import java.util.UUID
import play.api.test.FakeRequest
import org.joda.time.{DateTimeZone, DateTime}
import controllers.bt.testframework.mocks.DateTimeProviderMock

trait EmptySessionRequest extends CookieCrypto {

  def request = {
    val session = ("sessionId", encrypt(s"session-${UUID.randomUUID().toString}"))
    FakeRequest().withSession(session)
  }

}