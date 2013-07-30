package controllers

import microservice.sa.domain.{ SaPerson, SaRoot }
import views.html.sa.sa_personal_details

class SaController extends BaseController with ActionWrappers {

  def details = AuthorisedForAction() {
    implicit user =>
      implicit request =>

        val userData: SaRoot = user.regimes.sa.get

        userData.personalDetails match {
          case Some(person: SaPerson) => Ok(sa_personal_details(userData.utr, person, user.nameFromGovernmentGateway.getOrElse("")))
          case _ => NotFound //todo this should really be an error page
        }
  }

  def noEnrolment = AuthorisedForAction(None) {
    user =>
      request =>
        Ok("dear me")
  }

}
