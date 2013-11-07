package controllers.bt

import controllers.common._
import uk.gov.hmrc.common.PortalUrlBuilder
import controllers.bt.accountsummary._
import uk.gov.hmrc.common.microservice.domain.User
import views.helpers.LinkMessage
import play.api.mvc.Request
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import controllers.common.service.Connectors

class BusinessTaxController(accountSummaryFactory: AccountSummariesFactory,
                            override val auditConnector: AuditConnector)
                           (implicit override val authConnector: AuthConnector)
  extends BaseController2
  with Actions
  with PortalUrlBuilder {

  def this() = this(new AccountSummariesFactory(), Connectors.auditConnector)(Connectors.authConnector)

  def home = ActionAuthorisedBy(GovernmentGateway)() {
    user => request => businessTaxHomepage(user, request)
  }

  private[bt] def businessTaxHomepage(implicit user: User, request: Request[AnyRef]) = {
    val accountSummaries = accountSummaryFactory.create(buildPortalUrl)
    val otherServicesLink = LinkMessage.portalLink(buildPortalUrl("otherServices"))
    val enrolServiceLink = LinkMessage.portalLink(buildPortalUrl("otherServicesEnrolment"))
    val removeServiceLink = LinkMessage.portalLink(buildPortalUrl("servicesDeEnrolment"))
    Ok(views.html.business_tax_home(accountSummaries, otherServicesLink, enrolServiceLink, removeServiceLink))
  }


}
