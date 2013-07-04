package controllers

import microservice.sa.domain.{ SaPerson, SaRoot }

class SaController extends BaseController with ActionWrappers {

  def home = AuthorisedForAction {
    implicit user =>
      implicit request =>

        val userData: SaRoot = user.regimes.sa.get

        userData.personalDetails match {
          case Some(person: SaPerson) => Ok(views.html.sa_home(userData.utr, person.name, user.ggwName.get))
          case _ => NotFound //todo this should really be an error page
        }
  }

  def details = AuthorisedForAction {
    implicit user =>
      implicit request =>

        val userData: SaRoot = user.regimes.sa.get

        userData.personalDetails match {
          case Some(person: SaPerson) => Ok(views.html.sa_personal_details(person))
          case _ => NotFound //todo this should really be an error page
        }
  }

}
