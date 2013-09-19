package controllers.common

import play.Logger


trait Redirect  {
  protected val name : String

  def unapply(target: String) : Boolean = target match {
    case `name`  => true
    case  _ => false
  }

  def apply() = name
}

object RegisterUserRedirect extends Redirect {
  override protected val name = "register agent"
}

object CarBenefitHomeRedirect extends Redirect {
  override protected val name = "car benefit"
}

object RedirectUtils extends BaseController {

  val payeHome = "/paye/home"
  val saHome = "/sa/home"
  val businessTaxHome = "/business-tax/home"
  val agentHome = "/agent/home"
  val carBenefit = "/paye/benefit-overview"
  val agentRegistration = "/agent/reason-for-application"

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
    Logger.debug("Redirecting to benefit-overview")
    Redirect(carBenefit)
  }
}