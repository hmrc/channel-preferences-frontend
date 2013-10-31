package controllers.bt

import controllers.bt.otherservices.{OtherServicesFactory, OtherServicesSummary}
import controllers.common.{Actions, GovernmentGateway, BaseController}
import uk.gov.hmrc.common.PortalUrlBuilder
import uk.gov.hmrc.common.microservice.domain.User
import play.api.templates.Html
import controllers.common.service.MicroServices
import play.api.mvc.Request


class OtherServicesController(otherServicesFactory: OtherServicesFactory = new OtherServicesFactory(MicroServices.governmentGatewayMicroService))
  extends BaseController
  with Actions
  with PortalUrlBuilder {

  def otherServices = ActionAuthorisedBy(GovernmentGateway)() {
    user => request => otherServicesPage(user, request)
  }

  private[bt] def otherServicesPage(implicit user: User, request: Request[AnyRef]) = {
    val otherServicesSummary = OtherServicesSummary(
      otherServicesFactory.createManageYourTaxes(buildPortalUrl),
      otherServicesFactory.createOnlineServicesEnrolment(buildPortalUrl),
      otherServicesFactory.createBusinessTaxesRegistration(buildPortalUrl)
    )
    Ok(views.html.other_services(otherServicesSummary))
  }
}
