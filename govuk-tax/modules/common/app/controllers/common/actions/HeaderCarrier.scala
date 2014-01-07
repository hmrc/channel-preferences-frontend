package controllers.common.actions

import controllers.common.{HeaderNames, CookieCrypto}
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
                         requestId: Option[String] = None) {

  val nsStamp = System.nanoTime()

  /**
   * @return the time, in nanoseconds, since this header carrier was created
   */
  def elapsedNs = System.nanoTime() - nsStamp

  val names = HeaderNames
  lazy val headers: Seq[(String, String)] = {
    List(userId.map(u => names.authorisation -> s"Bearer $u"),
      token.map(t => SessionKeys.tokenName -> t),
      requestId.map(rid => names.xRequestId -> rid),
      forwarded.map(fo => names.forwardedFor -> fo),
      sessionId.map(sid => names.xSessionId -> sid)).flatten.toList
  }
}

trait SessionKeys {
  val sessionIdName = "sessionId"
  val userIdName = "userId"
  val tokenName = "token"
}

object SessionKeys extends SessionKeys

object HeaderCarrier extends CookieCrypto {
  val names = HeaderNames

  import SessionKeys._

  def apply(request: Request[_]) = {
    val userId = request.session.get(userIdName).map(decrypt)
    val token = request.session.get(tokenName)
    val forwardedFor = request.headers.get(names.forwardedFor)
    val sessionId = request.session.get(sessionIdName).map(decrypt)

    val requestIdString = request.headers.get(names.xRequestId)

    new HeaderCarrier(userId, token, forwardedFor, sessionId, requestIdString)
  }
}