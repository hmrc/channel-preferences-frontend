package controllers.common

import config.DateTimeProvider
import scala.Some
import org.joda.time.{DateTimeZone, Duration, DateTime}
import play.api.Logger
import concurrent.Future
import controllers.common.actions.HeaderCarrier
import play.api.mvc.Session

trait SessionTimeoutWrapper extends DateTimeProvider {

  object WithSessionTimeoutValidation extends WithSessionTimeoutValidation(now)

  object WithNewSessionTimeout extends WithNewSessionTimeout(now)

}

object SessionTimeoutWrapper {
  val timeoutSeconds = 900

  def hasValidTimestamp(session: Session, now: () => DateTime): Boolean = {
    val valid: Option[Boolean] = for {
      lastRequestTimestamp: DateTime <- extractTimestamp(session)
      sessionExpiryTimestamp: DateTime <- Some(lastRequestTimestamp.plus(Duration.standardSeconds(timeoutSeconds)))
      if now().isBefore(sessionExpiryTimestamp)
    } yield true

    valid.isDefined
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

  import play.api.mvc._
  import SessionTimeoutWrapper._
  import play.api.libs.concurrent.Execution.Implicits._

  def apply(action: Action[AnyContent]): Action[AnyContent] = Action.async {
    implicit request: Request[AnyContent] => {

      val result = if (hasValidTimestamp(request.session, now)) {
        action(request)
      } else {
        Logger.info(s"request refused as the session had timed out in ${request.path}")
        AnyAuthenticationProvider.redirectToLogin(false).flatMap { simpleResult =>
          Action(simpleResult)(request).map(_.withNewSession)
        }
      }
      addTimestamp(request, result)
    }
  }

}

class WithNewSessionTimeout(val now: () => DateTime) extends SessionTimeout {

  import play.api.mvc._

  def apply(action: Action[AnyContent]) = Action.async {
    request: Request[AnyContent] => {

      val result = action(request)
      addTimestamp(request, result)
    }
  }

}

trait SessionTimeout {

  import play.api.mvc._
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