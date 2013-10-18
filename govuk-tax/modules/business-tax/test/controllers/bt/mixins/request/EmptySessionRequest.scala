package controllers.bt.mixins.request

import controllers.common.CookieEncryption
import java.util.UUID
import play.api.test.FakeRequest
import org.joda.time.{DateTimeZone, DateTime}
import controllers.bt.mixins.mocks.DateTimeProviderMock

trait EmptySessionRequest extends CookieEncryption {

  self: DateTimeProviderMock =>

  implicit lazy val request = {
    val session = ("sessionId", encrypt(s"session-${UUID.randomUUID().toString}"))
    FakeRequest().withSession(session)
  }

  override def currentTime: DateTime = new DateTime(2013, 2, 11, 11, 55, 22, 555, DateTimeZone.UTC)
}