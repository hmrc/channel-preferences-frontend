package controllers.common

import config.DateTimeProvider
import scala.Some
import org.joda.time.DateTime

trait SessionTimeoutWrapper extends DateTimeProvider{
  object WithSessionTimeoutValidation extends WithSessionTimeoutValidation(now)
  object WithNewSessionTimeout extends WithNewSessionTimeout(now)
}

object SessionTimeoutWrapper {
  val sessionTimestampKey = "ts"
  val timeoutSeconds = 900
}

class WithSessionTimeoutValidation(val now: () => DateTime) extends SessionTimeout {

  import play.api.mvc._
  import org.joda.time.{Duration, DateTimeZone, DateTime}
  import SessionTimeoutWrapper._

  val defaultErrorAction: Action[AnyContent] = Action(Results.Redirect(routes.HomeController.landing()))

  def apply(errorResult: Action[AnyContent], action: Action[AnyContent]): Action[AnyContent] = Action {
    request: Request[AnyContent] => {

      val result = if (hasValidTimestamp(request.session)) {
        action(request)
      } else {
        errorResult(request).withNewSession
      }
      addTimestamp(request, result)
    }
  }

  def apply(action: Action[AnyContent]): Action[AnyContent] = apply(defaultErrorAction, action)


  private def hasValidTimestamp(session: Session): Boolean = {
    val valid: Option[Boolean] = for {
      lastRequestTimestamp: DateTime <- extractTimestamp(session)
      sessionExpiryTimestamp: DateTime <- Some(lastRequestTimestamp.withDurationAdded(Duration.standardSeconds(timeoutSeconds), 1))
      if now().isBefore(sessionExpiryTimestamp)
    } yield true

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

class WithNewSessionTimeout(val now: () => DateTime) extends SessionTimeout {

  import play.api.mvc._

  def apply(action: Action[AnyContent]) = Action {
    request: Request[AnyContent] => {

      val result = action(request)
      addTimestamp(request, result)
    }
  }

}

trait SessionTimeout {

  import play.api.mvc._
  import org.joda.time.DateTime
  import scala.concurrent.ExecutionContext
  import ExecutionContext.Implicits.global
  import play.api.mvc.AsyncResult
  import SessionTimeoutWrapper._
  import play.api.http.HeaderNames.SET_COOKIE

  val now : () => DateTime

  protected def addTimestamp(request: Request[AnyContent], result: Result) = {
    result match {
      case plain: PlainResult => insertTimestampNow(request, plain)
      case async: AsyncResult => async.transform(insertTimestampNow(request, _))
    }
  }

  private def insertTimestampNow(request: Request[AnyContent], result: PlainResult): Result = {
    val sessionData = sessionFromResultOrRequest(request, result).data.toSeq
    val newSessionData = sessionData :+ (sessionTimestampKey -> now().getMillis.toString)
    result.withSession(newSessionData: _*)
  }

  private def sessionFromResultOrRequest(request: Request[AnyContent], result: PlainResult): Session = {
    result.header.headers.get(SET_COOKIE) match {
      case None => request.session
      case cookieHeader => Session.decodeFromCookie(Cookies(cookieHeader).get(Session.COOKIE_NAME))
    }
  }
}