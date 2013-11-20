package controllers.common.actions

import controllers.common.{HeaderNames, CookieEncryption}
import play.api.mvc.Request
import java.util.UUID

/**
 * We need a way to carry header information from the request coming in
 * to Play through the system for use when making calls to the microservices.
 * This includes session and authentication information.
 *
 * The companion object can construct a HeaderCarrier from a Request object,
 * and the HeaderCarrier can build a Seq of headers from the data it holds.
 */
case class HeaderCarrier(userId: Option[String] = None,
                         token: Option[String] = None,
                         forwardedFor: Option[String] = None,
                         sessionId: Option[String] = None,
                         appName: String = "govuk-tax") {
  lazy val requestId = s"$appName-${UUID.randomUUID().toString}"

  lazy val headers: Seq[(String, String)] = {
    (HeaderNames.requestId -> requestId) +:
      List(userId.map(u => "userId" -> u),
        token.map(t => "token" -> t),
        forwardedFor.map(f => HeaderNames.forwardedFor -> f),
        sessionId.map(s => "sessionId" -> s)).flatten.toList
  }
}

object HeaderCarrier extends CookieEncryption with HeaderNames {
  def apply(request: Request[_]): HeaderCarrier = {
    val userId = request.session.get("userId").map(decrypt)
    val token = request.session.get("token")
    val forwardedForO = request.headers.get(forwardedFor)
    val sessionId = request.session.get("sessionId").map(sessionId => decrypt(sessionId))

    new HeaderCarrier(userId, token, forwardedForO, sessionId)
  }
}