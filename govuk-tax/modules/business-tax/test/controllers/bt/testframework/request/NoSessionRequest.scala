package controllers.bt.testframework.request

import play.api.test.FakeRequest
import org.joda.time.{DateTimeZone, DateTime}
import controllers.bt.testframework.mocks.DateTimeProviderMock

trait NoSessionRequest {
  def request = FakeRequest()
}
