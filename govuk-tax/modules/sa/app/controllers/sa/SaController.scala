package controllers.sa

import uk.gov.hmrc.microservice.sa.domain.{ SaRegime, SaPerson, SaRoot }
import views.html.sa.sa_personal_details
import controllers.common.{ SessionTimeoutWrapper, ActionWrappers, BaseController }

class SaController extends BaseController with ActionWrappers with SessionTimeoutWrapper {

  def details = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction(Some(SaRegime)) {
    implicit user =>
      implicit request =>

        val userData: SaRoot = user.regimes.sa.get

        userData.personalDetails match {
          case Some(person: SaPerson) => Ok(sa_personal_details(userData.utr, person, user.nameFromGovernmentGateway.getOrElse("")))
          case _ => NotFound //todo this should really be an error page
        }
  })

}
