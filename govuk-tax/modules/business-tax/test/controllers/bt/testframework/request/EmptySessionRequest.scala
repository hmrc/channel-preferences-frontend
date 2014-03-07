package controllers.bt.testframework.request

import java.util.UUID
import play.api.test.FakeRequest
import controllers.common.SessionKeys

trait EmptySessionRequest {

  def request = FakeRequest().withSession((SessionKeys.sessionId, s"session-${UUID.randomUUID()}"))

}