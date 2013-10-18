package controllers.bt

import controllers.bt.otherServices.OtherServicesSummary
import controllers.common.{ActionWrappers, BaseController}
import uk.gov.hmrc.common.PortalDestinationUrlBuilder
import uk.gov.hmrc.common.microservice.domain.User
import controllers.bt.otherServicesViews.OtherServicesFactory


class OtherServicesController(otherServicesFactory: OtherServicesFactory) extends BaseController
with ActionWrappers
with PortalDestinationUrlBuilder {

  def this() = this(new OtherServicesFactory)

  def otherServices = AuthorisedForGovernmentGatewayAction() {
    implicit user =>
      implicit request =>
        Ok(otherServicesPage(otherServicesFactory.create(user)))
  }

  private[bt] def otherServicesPage(otherServicesSummary : OtherServicesSummary)(implicit user: User) =
    views.html.other_services(otherServicesSummary)


}
