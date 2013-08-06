package controllers

import play.api.mvc._
import org.joda.time.{ Duration, DateTimeZone, DateTime }
import config.DateTimeProvider
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import play.Logger

trait SessionTimeoutWrapper extends DateTimeProvider {
  self: Controller =>

  import SessionTimeoutWrapper._

  object WithSessionTimeoutValidation {

    def apply(action: Action[AnyContent]) = Action {
      request: Request[AnyContent] =>
        {

          val result = if (hasValidTimestamp(request.session)) {
            action(request)
          } else {
            Results.Redirect(routes.HomeController.landing()).withNewSession
          }

          addTimestamp(request, result)
        }
    }
  }

  object WithNewSessionTimeout {

    def apply(action: Action[AnyContent]) = Action {
      request: Request[AnyContent] =>
        {

          val result = action(request)
          addTimestamp(request, result)
        }
    }
  }

  object ValidateSession {
    def apply() = Action {
      request: Request[AnyContent] =>
        {
          if (hasValidTimestamp(request.session)) {
            Ok("ok")
          } else {
            Unauthorized("unauthorised")
          }
        }
    }
  }

  private def hasValidTimestamp(session: Session): Boolean = {
    val valid: Option[Boolean] = for {
      lastRequestTimestamp: DateTime <- extractTimestamp(session)
      sessionExpiryTimestamp: DateTime <- Some(lastRequestTimestamp.withDurationAdded(Duration.standardSeconds(timeoutSeconds), 1))
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

  private def addTimestamp(request: Request[AnyContent], result: Result) = {
    result match {
      case plain: PlainResult => insertTimestampNow(request, plain)
      case async: AsyncResult => async.transform(insertTimestampNow(request, _))
    }

  }

  private def insertTimestampNow(request: Request[AnyContent], result: PlainResult): Result = {
    val newSessionData = sessionFromResultOrRequest(request, result) :+ (sessionTimestampKey -> now().getMillis.toString)
    result.withSession(newSessionData: _*)
  }

  private def sessionFromResultOrRequest(request: Request[AnyContent], result: PlainResult): Seq[(String, String)] = {
    val sessionOrNot: Option[String] = result.header.headers.get(SET_COOKIE)
    if (sessionOrNot.isDefined) {
      Session.decodeFromCookie(Cookies(sessionOrNot).get(Session.COOKIE_NAME)).data.toSeq
    } else request.session.data.toSeq
  }
}

object SessionTimeoutWrapper {
  val sessionTimestampKey = "ts"
  val timeoutSeconds = 10
}
