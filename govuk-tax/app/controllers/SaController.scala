package controllers

import microservice.sa.domain.{ SaPerson, SaRoot }
import org.joda.time.DateTime
import views.html.sa._

class SaController extends BaseController with ActionWrappers {

  def home = AuthorisedForAction {
    implicit user =>
      implicit request =>

        val userData: SaRoot = user.regimes.sa.get

        userData.personalDetails match {
          case Some(person: SaPerson) => {
            val previouslyLoggedIn: Option[DateTime] = user.userAuthority.previouslyLoggedInAt
            Ok(sa_home(userData.utr, person.name, user.ggwName.getOrElse(""), previouslyLoggedIn))
          }
          case _ => NotFound //todo this should really be an error page
        }
  }

  def details = AuthorisedForAction {
    implicit user =>
      implicit request =>

        val userData: SaRoot = user.regimes.sa.get

        userData.personalDetails match {
          case Some(person: SaPerson) => Ok(sa_personal_details(userData.utr, person, user.ggwName.getOrElse("")))
          case _ => NotFound //todo this should really be an error page
        }
  }

}
