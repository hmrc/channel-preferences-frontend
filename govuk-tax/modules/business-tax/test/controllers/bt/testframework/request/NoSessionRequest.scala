package controllers.bt.testframework.request

import play.api.test.FakeRequest
import org.joda.time.{DateTimeZone, DateTime}
import controllers.bt.testframework.mocks.DateTimeProviderMock

trait NoSessionRequest {

  self: DateTimeProviderMock =>

  implicit lazy val request = FakeRequest()
  override def currentTime: DateTime = new DateTime(2013, 2, 11, 11, 55, 22, 555, DateTimeZone.UTC)
}
