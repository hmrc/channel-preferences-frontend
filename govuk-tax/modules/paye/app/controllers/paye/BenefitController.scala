package controllers.paye

import play.api.mvc.Request
import scala.concurrent._


import uk.gov.hmrc.common.microservice.paye.PayeConnector
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import controllers.common.actions.{Actions, HeaderCarrier}
import controllers.paye.validation.BenefitFlowHelper._
import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.SimpleResult
import controllers.common.BaseController

trait BenefitController extends BaseController with Actions with PayeRegimeRoots {
  def benefitController(body: (User, Request[_]) => Future[SimpleResult])(implicit payeConnector: PayeConnector) = AuthorisedFor(PayeRegime).async {
    implicit user =>
      implicit request =>
        implicit val hc = HeaderCarrier(request)
        validateVersionNumber(user, request.session).flatMap {
          _.fold(
            errorResult => Future.successful(errorResult),
            versionNumber => body(user, request))
        }
  }
}
