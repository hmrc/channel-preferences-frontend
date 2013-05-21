package controllers

import controllers.service.TaxUser
import play.api.mvc.Action

object Payments extends Payments(TaxUser())

private[controllers] class Payments(taxUser: TaxUser) extends BaseController {

  def outstanding(pid: String) = Action { Ok("all is ok....").as(JSON) }

}

