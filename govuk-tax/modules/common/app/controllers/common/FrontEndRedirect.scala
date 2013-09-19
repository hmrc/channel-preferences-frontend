package controllers.common

import play.Logger
import play.api.mvc.Session


trait RedirectCommand  {
  protected val name : String

  def unapply(target: String) : Boolean = target match {
    case `name`  => true
    case  _ => false
  }

  def apply() = name
}

object RegisterUserRedirect extends RedirectCommand {
  override protected val name = "register agent"
}

object CarBenefitHomeRedirect extends RedirectCommand {
  override protected val name = "car benefit"
}

object FrontEndRedirect extends BaseController {

  val payeHome = "/paye/home"
  val saHome = "/sa/home"
  val businessTaxHome = "/business-tax/home"
  val agentHome = "/agent/home"
  val carBenefit = "/paye/car-benefit-home"
  val agentRegistration = "/agent/reason-for-application"


  def forSession(session: Session) = {
    session.data.get("login_redirect") match {
      case Some(RegisterUserRedirect()) => toAgent
      case Some(CarBenefitHomeRedirect()) => toCarBenefit
      case None => toPaye //todo wwhat is the default destination?
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
  def toAgent = {
    Logger.debug("Redirecting to agent...")
    Redirect(agentHome)
  }

  def toSamlLogin = {
    Logger.debug("Redirecting to login")
    Redirect(routes.LoginController.samlLogin)
  }

  def toCarBenefit = {
    Logger.debug("Redirecting to car-benefit-home")
    Redirect(carBenefit)
  }
}