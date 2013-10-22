package controllers.bt

import controllers.bt.otherservices.{OtherServicesFactory, OtherServicesSummary}
import controllers.common.{ActionWrappers, BaseController}
import uk.gov.hmrc.common.PortalUrlBuilder
import uk.gov.hmrc.common.microservice.domain.User
import play.api.templates.Html


class OtherServicesController(otherServicesFactory: OtherServicesFactory)
  extends BaseController
  with ActionWrappers
  with PortalUrlBuilder {

  def this() = this(new OtherServicesFactory)

  def otherServices = AuthorisedForGovernmentGatewayAction() {
    implicit user =>
      implicit request =>
        Ok(otherServicesPage(
          OtherServicesSummary(
            otherServicesFactory.createManageYourTaxes,
            otherServicesFactory.createOnlineServicesEnrolment,
            otherServicesFactory.createBusinessTaxesRegistration
          )
        ))
  }

  private[bt] def otherServicesPage(otherServicesSummary: OtherServicesSummary)(implicit user: User): Html =
    views.html.other_services(otherServicesSummary)
}
