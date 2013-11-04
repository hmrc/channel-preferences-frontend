package controllers.bt

import controllers.bt.otherservices.{OtherServicesFactory, OtherServicesSummary}
import controllers.common.{Actions, GovernmentGateway, BaseController}
import uk.gov.hmrc.common.PortalUrlBuilder
import uk.gov.hmrc.common.microservice.domain.User
import controllers.common.service.Connectors
import play.api.mvc.Request


class OtherServicesController(otherServicesFactory: OtherServicesFactory)
  extends BaseController
  with Actions
  with PortalUrlBuilder {

  def this() = this(new OtherServicesFactory(Connectors.governmentGatewayConnector))

  def otherServices = ActionAuthorisedBy(GovernmentGateway)() {
    user => request => otherServicesPage(user, request)
  }

  private[bt] def otherServicesPage(implicit user: User, request: Request[AnyRef]) = {
    val otherServicesSummary = OtherServicesSummary(
      otherServicesFactory.createManageYourTaxes(buildPortalUrl),
      otherServicesFactory.createOnlineServicesEnrolment(buildPortalUrl),
      otherServicesFactory.createOnlineServicesDeEnrolment(buildPortalUrl),
      otherServicesFactory.createBusinessTaxesRegistration(buildPortalUrl)
    )
    Ok(views.html.other_services(otherServicesSummary))
  }
}
