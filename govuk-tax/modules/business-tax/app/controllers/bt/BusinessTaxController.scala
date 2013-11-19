package controllers.bt

import controllers.common._
import uk.gov.hmrc.common.PortalUrlBuilder
import controllers.bt.accountsummary._
import uk.gov.hmrc.common.microservice.domain.User
import views.helpers.LinkMessage
import play.api.mvc.{SimpleResult, Request}
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import controllers.common.service.Connectors
import controllers.common.actions.Actions
import scala.concurrent._
import ExecutionContext.Implicits.global

class BusinessTaxController(accountSummaryFactory: AccountSummariesFactory,
                            override val auditConnector: AuditConnector)
                           (implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with PortalUrlBuilder {

  def this() = this(new AccountSummariesFactory(), Connectors.auditConnector)(Connectors.authConnector)

  def home = AsyncAuthenticatedBy(GovernmentGateway) {
    user => request => businessTaxHomepage(user, request)
  }

  private[bt] def businessTaxHomepage(implicit user: User, request: Request[AnyRef]): Future[SimpleResult] = {
    val accountSummariesF = accountSummaryFactory.create(buildPortalUrl)
    val otherServicesLink = LinkMessage.internalLink(controllers.bt.routes.OtherServicesController.otherServices.url, "link")
    val enrolServiceLink = LinkMessage.portalLink(buildPortalUrl("otherServicesEnrolment"))
    val removeServiceLink = LinkMessage.portalLink(buildPortalUrl("servicesDeEnrolment"))
    accountSummariesF.map { accountSummaries =>
      Ok(views.html.business_tax_home(accountSummaries, otherServicesLink, enrolServiceLink, removeServiceLink))
    }
  }


}
