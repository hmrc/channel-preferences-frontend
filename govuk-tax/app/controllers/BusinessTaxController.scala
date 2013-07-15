package controllers

import microservice.sa.domain.{ SaPerson, SaRoot }
import org.joda.time.DateTime

class BusinessTaxController extends BaseController with ActionWrappers {

  def home = AuthorisedForAction {
    implicit user =>
      implicit request =>

        val userData: SaRoot = user.regimes.sa.get

        userData.personalDetails match {
          case Some(person: SaPerson) => {
            val previouslyLoggedIn: Option[DateTime] = user.userAuthority.previouslyLoggedInAt
            Ok(views.html.business_tax_home(user.regimes, userData.utr, person.name, user.ggwName.getOrElse(""), previouslyLoggedIn))
          }
          case _ => NotFound //todo this should really be an error page
        }
  }
}
