package controllers.common

import play.Logger
import play.api.mvc.{Controller, Session}

object FrontEndRedirect extends Controller {

  val businessTaxHome = "/account"

  def carBenefit(token:Option[String]) = token.map { token =>
    s"/paye/company-car/details?token=$token"
  }.getOrElse {
    "/paye/company-car/landing-redirect"

  }
  val carBenefitStartPage = "/paye/company-car"

  //TODO what is the default destination?
  private val defaultDestination = carBenefit(None)

  def forSession(session: Session) =
    Redirect(session.data.get(SessionKeys.redirect).getOrElse(defaultDestination)).withSession(session - SessionKeys.redirect)


  def buildSessionForRedirect(session: Session, redirectTo: Option[String]) =
    session.copy(session.data ++ redirectTo.map(SessionKeys.redirect -> _).toMap)

  def toBusinessTax = {
    Logger.debug("Redirecting to business...")
    Redirect(businessTaxHome)
  }

  def toSamlLogin = {
    Logger.debug("Redirecting to login")
    Redirect(routes.IdaLoginController.samlLogin)
  }

}