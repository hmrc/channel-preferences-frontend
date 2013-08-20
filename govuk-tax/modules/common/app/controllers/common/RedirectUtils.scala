package controllers.common

import play.Logger

object RedirectUtils extends BaseController {

  val payeHome = "/paye/home"
  val saHome = "/sa/home"
  val businessTaxHome = "/business-tax/home"
  val agentHome = "/agent/home"
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

}
