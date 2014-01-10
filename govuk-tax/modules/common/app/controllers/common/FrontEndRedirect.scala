package controllers.common

import play.Logger
import play.api.mvc.{Controller, Session}

object FrontEndRedirect extends Controller {

  val businessTaxHome = "/business-tax"
  val carBenefit = "/paye/car-benefit"
  val payeHome = "/paye"

  //TODO what is the default destination?
  private val defaultDestination = payeHome

  def forSession(session: Session) =
    Redirect(session.data.get(SessionKeys.redirect).getOrElse(defaultDestination)).withSession(session - SessionKeys.redirect)


  def buildSessionForRedirect(session: Session, redirectTo: Option[String]) =
    session.copy(session.data ++ redirectTo.map(SessionKeys.redirect -> _).toMap)

  def toPaye = {
    Logger.debug("Redirecting to paye...")
    Redirect(payeHome)
  }

  def toBusinessTax = {
    Logger.debug("Redirecting to business...")
    Redirect(businessTaxHome)
  }

  def toSamlLogin = {
    Logger.debug("Redirecting to login")
    Redirect(routes.LoginController.samlLogin)
  }

}