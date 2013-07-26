package controllers

import microservice.sa.domain.{ SaPerson, SaRoot }
import org.joda.time.DateTime

class BusinessTaxController extends BaseController with ActionWrappers {

  def home = AuthorisedForAction {
    implicit user =>
      implicit request =>

        val userAuthority = user.userAuthority

        val previouslyLoggedIn: Option[DateTime] = userAuthority.previouslyLoggedInAt

        Ok(views.html.business_tax_home(user.regimes, userAuthority.utr, userAuthority.vrn, user.nameFromGovernmentGateway.getOrElse(""), previouslyLoggedIn))

  }
}
