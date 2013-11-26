package controllers.bt

import controllers.bt.otherservices.{OtherServicesFactory, OtherServicesSummary}
import controllers.common.{BaseController, GovernmentGateway}
import uk.gov.hmrc.common.PortalUrlBuilder
import uk.gov.hmrc.common.microservice.domain.User
import controllers.common.service.Connectors
import play.api.mvc.Request
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import controllers.common.actions.Actions

class OtherServicesController(otherServicesFactory: OtherServicesFactory,
                              override val auditConnector: AuditConnector)
                             (implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with PortalUrlBuilder
  with BusinessTaxRegimeRoots {

  def this() = this(new OtherServicesFactory(Connectors.governmentGatewayConnector), Connectors.auditConnector)(Connectors.authConnector)

  def otherServices = AuthenticatedBy(GovernmentGateway).async {
    user => request => otherServicesPage(user, request)
  }

  private[bt] def otherServicesPage(implicit user: User, request: Request[AnyRef]) = {

    val manageYourTaxesF = otherServicesFactory.createManageYourTaxes(buildPortalUrl)
    val enrolment = otherServicesFactory.createOnlineServicesEnrolment(buildPortalUrl)
    val deEnrolment = otherServicesFactory.createOnlineServicesDeEnrolment(buildPortalUrl)
    val businessTaxesRegistration = otherServicesFactory.createBusinessTaxesRegistration(buildPortalUrl)

    manageYourTaxesF.map {
      manageYourTaxes =>
        val summary = OtherServicesSummary(
          manageYourTaxes,
          enrolment,
          deEnrolment,
          businessTaxesRegistration
        )
        Ok(views.html.other_services(summary))
    }


  }
}
