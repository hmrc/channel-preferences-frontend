package controllers

import microservice.sa.domain.{ SaPerson, SaRoot }
import org.joda.time.DateTime

class SaController extends BaseController with ActionWrappers {

  def details = AuthorisedForAction {
    implicit user =>
      implicit request =>

        val userData: SaRoot = user.regimes.sa.get

        userData.personalDetails match {
          case Some(person: SaPerson) => Ok(views.html.sa_personal_details(userData.utr, person, user.ggwName.getOrElse("")))
          case _ => NotFound //todo this should really be an error page
        }
  }

}
