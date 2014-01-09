package controllers.common.actions

import controllers.common.{SessionKeys, HeaderNames, CookieCrypto}
import play.api.mvc.Request
import scala.util.Try

trait LoggingDetails {

  def sessionId: Option[String]

  def requestId: Option[String]

  lazy val data = Map(
    (HeaderNames.xRequestId, requestId),
    (HeaderNames.xSessionId, sessionId))

  def mdcData: Map[String, String]= for {
    d <- data
    v <- d._2
  } yield (d._1, v)
}

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
                         requestId: Option[String] = None,
                         nsStamp: Long = System.nanoTime()) extends LoggingDetails {

  /**
   * @return the time, in nanoseconds, since this header carrier was created
   */
  def age = System.nanoTime() - nsStamp

  val names = HeaderNames
  lazy val headers: Seq[(String, String)] = {
    List(userId.map(u => names.authorisation -> s"Bearer $u"),
      token.map(t => SessionKeys.token -> t),
      requestId.map(rid => names.xRequestId -> rid),
      forwarded.map(fo => names.forwardedFor -> fo),
      sessionId.map(sid => names.xSessionId -> sid)).flatten.toList
  }
}

object HeaderCarrier extends CookieCrypto {
  val names = HeaderNames

  def apply(request: Request[_]) = {
    val userId = request.session.get(SessionKeys.userId).map(decrypt)
    val token = request.session.get(SessionKeys.token)
    val forwardedFor = request.headers.get(names.forwardedFor)
    val sessionId = request.session.get(SessionKeys.sessionId).map(decrypt)

    val requestTimestamp = Try[Long] {
      request.session.get(names.xRequestTimestamp).map(_.toLong).getOrElse(System.nanoTime())
    }
    val requestIdString = request.headers.get(names.xRequestId)

    new HeaderCarrier(userId, token, forwardedFor, sessionId, requestIdString, requestTimestamp.toOption.getOrElse(System.nanoTime()))
  }
}
