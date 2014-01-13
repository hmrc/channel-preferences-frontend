package controllers.paye

import play.api.mvc._
import scala.concurrent._

import uk.gov.hmrc.common.microservice.paye.PayeConnector
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import controllers.common.actions.{Actions, HeaderCarrier}
import controllers.paye.validation.BenefitFlowHelper._
import controllers.common.BaseController
import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.SimpleResult

trait BenefitController extends BaseController with Actions with PayeRegimeRoots {
  type BodyWithVersion = (User, Request[_], Int) => Future[SimpleResult]
  type BodyWithoutVersion = (User, Request[_]) => Future[SimpleResult]

  def benefitController(body: BodyWithoutVersion)(implicit payeConnector: PayeConnector): Action[AnyContent] =
    benefitController { (user: User, request: Request[_], _: Int) => body(user, request)}

  def benefitController(body: BodyWithVersion)(implicit payeConnector: PayeConnector): Action[AnyContent] = AuthorisedFor(PayeRegime).async {
    implicit user =>
      implicit request =>
        implicit val hc = HeaderCarrier(request)
        validateVersionNumber(user, request.session).flatMap {
          _.fold(
            errorResult => Future.successful(errorResult),
            version => body(user, request, version))
        }
  }
}
