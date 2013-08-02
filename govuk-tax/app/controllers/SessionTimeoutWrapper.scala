package controllers

import play.api.mvc._
import microservice.domain.{ User, TaxRegime }
import microservice.domain.User
import play.api.mvc._
import controllers.service._
import microservice.domain.{ RegimeRoots, TaxRegime, User }
import microservice.auth.domain.Regimes
import play.Logger
import java.net.URI
import org.slf4j.MDC
import java.util.UUID
import views.html.{ login, server_error }
import play.api.{ Mode, Play }
import com.google.common.net.HttpHeaders
import microservice.HasResponse
import org.joda.time.{Duration, DateTimeZone, DateTime}
import config.DateTimeProvider

trait SessionTimeoutWrapper extends DateTimeProvider {

  self: Controller =>

  object WithSessionTimeout {

    private val sessionTimestampKey = "ts"
    private val timeoutSeconds = 900

    def apply(action: Action[AnyContent]) = Action {
      request: Request[AnyContent] => {

        val result = if (hasValidTimestamp(session)) {
          action(request)
        } else {
          Results.Redirect(routes.HomeController.landing()).withNewSession
        }

        result.withSession(session + (sessionTimestampKey -> now().getMillis.toString))
      }
    }

    private def hasValidTimestamp(session: Session): Boolean = {
      val valid: Option[Boolean] = for {
        lastRequestTimestamp: DateTime <- extractTimestamp(session)
        sessionExpiryTimestamp: DateTime <- lastRequestTimestamp.withDurationAdded(Duration.standardSeconds(timeoutSeconds), 1)
        if now().isBefore(sessionExpiryTimestamp)
      } yield {
        true
      }

      valid.isDefined
    }

    private def extractTimestamp(session: Session): Option[DateTime] = {
      try {
        session.get(sessionTimestampKey) map (timestamp => new DateTime(timestamp.toLong, DateTimeZone.UTC))
      } catch {
        case e: NumberFormatException => None
      }
    }
  }
}

//
//package config
//
//import play.api.mvc._
//import controllers.CookieEncryption
//import org.joda.time.{ DateTimeZone, Duration, DateTime }
//import scala.concurrent.ExecutionContext.Implicits.global
//import org.apache.http.impl.cookie.CookieSpecBase
//
//object LastRequestTimestampFilter extends LastRequestTimestampFilter
//
//trait LastRequestTimestampFilter extends Filter with DateTimeProvider {
//
//  val sessionTimestampKey = "lastRequestTimestamp"
//
//  def apply(next: RequestHeader => Result)(requestHeader: RequestHeader) = {
//
//    val incomingSession = requestHeader.session
//
//    val sessionTimestamp = extractTimestamp(requestHeader)
//
//    val validIncomingSession = sessionTimestamp match {
//      case Some(timestamp) => LastRequestTimestampCookie(timestamp).isValid(now)
//      case None => false
//    }
//
//    val result = if (validIncomingSession) {
//      next(requestHeader)
//    } else {
//      next(deleteCookies(requestHeader))
//    }
//
//    def addTimestamp(result: PlainResult): Result = {
//
//      val outgoingSession = sessionFromResult(result)
//
//      val loggedOutForSomeReason = false
//
//      if (outgoingSession.isEmpty) {
//        if (!validIncomingSession) {
//          result
//        } else if (loggedOutForSomeReason) {
//          result
//        } else {
//          result.withSession(incomingSession + (sessionTimestampKey -> now().getMillis.toString))
//        }
//      } else {
//        result.withSession(outgoingSession + (sessionTimestampKey -> now().getMillis.toString))
//      }
//    }
//
//    result match {
//      case plain: PlainResult => addTimestamp(plain)
//      case async: AsyncResult => async.transform(addTimestamp)
//    }
//  }
//
//  private[config] def sessionFromResult(result: PlainResult): Session = {
//    val cookies: Cookies = Cookies(result.header.headers.get(play.api.http.HeaderNames.SET_COOKIE))
//    Session.decodeFromCookie(cookies.get(Session.COOKIE_NAME))
//  }
//
//  private def deleteCookies(requestHeader: RequestHeader): RequestHeader = {
//    val newHeaderData = requestHeader.headers.toMap.filterKeys(_ != "Cookie")
//
//    val replacementHeaders = new Headers {
//      override val data: Seq[(String, Seq[String])] = newHeaderData.toSeq
//    }
//
//    requestHeader.copy(headers = replacementHeaders)
//  }
//
//  private def extractTimestamp(requestHeader: RequestHeader): Option[DateTime] = {
//    try {
//      requestHeader.session.get(sessionTimestampKey) map (timestamp => new DateTime(timestamp.toLong, DateTimeZone.UTC))
//    } catch {
//      case e: NumberFormatException => None
//    }
//  }
//}
//
//case class LastRequestTimestampCookie(lastRequestTimestamp: DateTime) extends CookieEncryption {
//
//  import LastRequestTimestampCookie._
//
//  def toCookie: Cookie = Cookie(cookieName, cookieValue, Some(timeoutSeconds))
//
//  def isValid(now: () => DateTime): Boolean = {
//    val sessionExpiryTimestamp = lastRequestTimestamp.withDurationAdded(Duration.standardSeconds(timeoutSeconds), 1)
//    now().isBefore(sessionExpiryTimestamp)
//  }
//
//  private def cookieValue = encrypt(lastRequestTimestamp.getMillis.toString)
//}
//
//object LastRequestTimestampCookie extends CookieEncryption {
//
//  val cookieName = "PLAY_REQUEST"
//
//  val timeoutSeconds = 5
//
//  def apply(cookie: Cookie): Option[LastRequestTimestampCookie] = {
//    require(cookie.name == cookieName)
//    decryptCookie(cookie).map(LastRequestTimestampCookie(_))
//  }
//
//  private def decryptCookie(cookie: Cookie): Option[DateTime] = {
//    try {
//      Some(new DateTime(decrypt(cookie.value).toLong, DateTimeZone.UTC))
//    } catch {
//      case e: NumberFormatException => None
//    }
//  }
//}
