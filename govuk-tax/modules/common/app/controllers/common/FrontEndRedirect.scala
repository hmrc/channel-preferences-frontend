package controllers.common

import play.Logger
import play.api.mvc.{AnyContent, Controller, Session, Request}

object FrontEndRedirect extends Controller {

  val businessTaxHome = "/account"

  def forSession(session: Session)(implicit rq: Request[AnyContent]) = {
    session.data.get(SessionKeys.redirect) match {
      case Some(whereToRedirect) => SeeOther(whereToRedirect.toString).withSession(session - SessionKeys.redirect)
      case None => BadRequest(views.html.error())
    }
  }


  def buildSessionForRedirect(session: Session, redirectTo: Option[String]) =
    session.copy(session.data ++ redirectTo.map(SessionKeys.redirect -> _).toMap)

  def toBusinessTax = {
    Logger.debug("Redirecting to business...")
    Redirect(businessTaxHome)
  }

  def toSamlLogin(token: Option[String]) = {
    Logger.debug("Redirecting to login")
    Redirect(routes.IdaLoginController.samlLogin(token))
  }

}