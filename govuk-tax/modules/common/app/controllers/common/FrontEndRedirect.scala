package controllers.common

import play.Logger
import play.api.mvc.{Controller, Session}

object FrontEndRedirect extends Controller {

  val businessTaxHome = "/account"
  val carBenefit = "/paye/company-car/details"
  val carBenefitStartPage = "/paye/company-car"

  //TODO what is the default destination?
  private val defaultDestination = carBenefit

  def forSession(session: Session) =
    Redirect(session.data.get(SessionKeys.redirect).getOrElse(defaultDestination)).withSession(session - SessionKeys.redirect)


  def buildSessionForRedirect(session: Session, redirectTo: Option[String]) =
    session.copy(session.data ++ redirectTo.map(SessionKeys.redirect -> _).toMap)

  def toBusinessTax = {
    Logger.debug("Redirecting to business...")
    Redirect(businessTaxHome)
  }

}