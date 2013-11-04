package controllers.common

import play.Logger
import play.api.mvc.{Controller, Session}

object FrontEndRedirect extends Controller {

  val saHome = "/sa/home"
  val businessTaxHome = "/business-tax/home"
  val agentHome = "/agent/home"
  val agentAddClient = "/agent/add-client"
  val carBenefit = "/paye/car-benefit/home"
  val payeHome = carBenefit
  val agentRegistration = "/agent/reason-for-application"

  val redirectSessionKey = "login_redirect"

  def forSession(session: Session) = {
    (session.data.get(redirectSessionKey) match {
      case Some(redirectUrl) => Redirect(redirectUrl)
      case None => toPaye //todo what is the default destination?
    }).withSession(session - redirectSessionKey)
  }

  def buildSessionForRedirect(session: Session, redirectTo: Option[String]) = {
    redirectTo match {
      case Some(redirectUrl) => session + (redirectSessionKey -> redirectUrl)
      case None => session
    }
  }

  def toPaye = {
    Logger.debug("Redirecting to paye...")
    Redirect(payeHome)
  }

  def toSa = {
    Logger.debug("Redirecting to sa...")
    Redirect(saHome)
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