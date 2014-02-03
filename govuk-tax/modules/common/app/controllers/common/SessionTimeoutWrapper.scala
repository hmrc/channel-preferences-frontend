package controllers.common

import scala.concurrent._
import org.joda.time.{DateTimeZone, Duration, DateTime}
import play.api.mvc._
import play.api.Logger

import config.DateTimeProvider
import controllers.common.actions.HeaderCarrier

trait SessionTimeoutWrapper extends DateTimeProvider {

  object WithSessionTimeoutValidation extends WithSessionTimeoutValidation(now)

  object WithNewSessionTimeout extends WithNewSessionTimeout(now)

}

object SessionTimeoutWrapper {
  val timeoutSeconds = 900

  def hasValidTimestamp(session: Session, now: () => DateTime): Boolean = {
   def isTimestampValid(timestamp: DateTime): Boolean = {
     val timeOfExpiry = timestamp plus Duration.standardSeconds(timeoutSeconds)
     now() isBefore timeOfExpiry
   }
    extractTimestamp(session) map isTimestampValid getOrElse true
  }

  private def extractTimestamp(session: Session): Option[DateTime] = {
    try {
      session.get(SessionKeys.lastRequestTimestamp) map (timestamp => new DateTime(timestamp.toLong, DateTimeZone.UTC))
    } catch {
      case e: NumberFormatException => None
    }
  }
}

class WithSessionTimeoutValidation(val now: () => DateTime) extends SessionTimeout {

  import SessionTimeoutWrapper._
  import uk.gov.hmrc.common.MdcLoggingExecutionContext._

  def apply(authenticationProvider: AuthenticationProvider)(action: Action[AnyContent]): Action[AnyContent] = Action.async {
    implicit request: Request[AnyContent] => {
      implicit val loggingDetails = HeaderCarrier(request)
      val result = if (hasValidTimestamp(request.session, now)) {
        action(request)
      } else {
        Logger.info(s"request refused as the session had timed out in ${request.path}")
        authenticationProvider.handleSessionTimeout().flatMap { simpleResult =>
          Action(simpleResult)(request).map(_.withNewSession)
        }
      }
      addTimestamp(request, result)
    }
  }

}

class WithNewSessionTimeout(val now: () => DateTime) extends SessionTimeout {
  def apply(action: Action[AnyContent]) = Action.async {
    request: Request[AnyContent] => {
      addTimestamp(request, action(request))
    }
  }
}

trait SessionTimeout {

  import org.joda.time.DateTime
  import play.api.http.HeaderNames.SET_COOKIE
  import uk.gov.hmrc.common.MdcLoggingExecutionContext._

  val now: () => DateTime

  protected def addTimestamp(request: Request[AnyContent], result: Future[SimpleResult]): Future[SimpleResult] = {
    implicit val headerCarrier = HeaderCarrier(request)
    result.map(insertTimestampNow(request))
  }

  private def insertTimestampNow(request: Request[AnyContent])(result: SimpleResult): SimpleResult = {
    val sessionData = sessionFromResultOrRequest(request, result).data.toSeq
    val newSessionData = sessionData :+ (SessionKeys.lastRequestTimestamp -> now().getMillis.toString)
    result.withSession(newSessionData: _*)
  }

  private def sessionFromResultOrRequest(request: Request[AnyContent], result: SimpleResult): Session = {
    result.header.headers.get(SET_COOKIE) match {
      case None => request.session
      case cookieHeader => Session.decodeFromCookie(Cookies(cookieHeader).get(Session.COOKIE_NAME))
    }
  }
}