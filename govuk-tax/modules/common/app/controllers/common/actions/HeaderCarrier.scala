package controllers.common.actions

import controllers.common.{HeaderNames, CookieEncryption}
import play.api.mvc.Request

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
                         forwarded: Option[String] = None,
                         sessionId: Option[String] = None,
                         requestId: Option[String]= None) {

  val names = HeaderNames
  lazy val headers: Seq[(String, String)] = {
    List(userId.map(u => names.authorisation -> s"Bearer $u"),
      token.map(names.token ->),
      requestId.map(names.xRequestId ->),
      forwarded.map(names.forwardedFor ->),
      sessionId.map(names.xSessionId ->)).flatten.toList
  }
}

object HeaderCarrier extends CookieEncryption {
  val names = HeaderNames

  def apply(request: Request[_]) = {
    val userId = request.session.get(names.userId).map(decrypt)
    val token = request.session.get(names.token)
    val forwardedFor = request.headers.get(names.forwardedFor)
    val sessionId = request.session.get(names.xSessionId).map(decrypt)

    val requestIdString = request.headers.get(names.xRequestId)

    new HeaderCarrier(userId, token, forwardedFor, sessionId, requestIdString)
  }
}