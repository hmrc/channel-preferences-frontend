package controllers.paye

import controllers.common.{CarBenefitHomeRedirect, SessionTimeoutWrapper, ActionWrappers, BaseController}
import play.api.mvc.{Result, Request, Results}
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime

class CarBenefitHomeController extends BaseController with ActionWrappers with SessionTimeoutWrapper {

  def carBenefitHome = WithSessionTimeoutValidation {
    AuthorisedForIdaAction(taxRegime = Some(PayeRegime), redirectCommand = Some(CarBenefitHomeRedirect)) {
        user => request => carBenefitHomeAction(user, request)
    }
  }

  private[paye] val carBenefitHomeAction: ((User, Request[_]) => Result) = (user, request) => {
    //TODO complete
    Ok(views.html.paye.car_benefit_home(None, None, None))
  }
}
