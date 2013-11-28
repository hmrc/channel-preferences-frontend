package controllers.bt

import controllers.common._
import uk.gov.hmrc.common.PortalUrlBuilder
import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.{SimpleResult, Request}
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import controllers.common.service.Connectors
import controllers.common.actions.Actions
import views.helpers.{PortalLink, LinkMessage}
import play.api.i18n.Messages

class MergeGGAccountsController(override val auditConnector: AuditConnector)(implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with PortalUrlBuilder
  with BusinessTaxRegimeRoots {

  def this() = this(Connectors.auditConnector)(Connectors.authConnector)

  def mergeGGAccounts = AuthenticatedBy(GovernmentGateway) {
    user => request => mergeGGAccountsPage(user, request)
  }

  private[bt] def mergeGGAccountsPage(implicit user: User, request: Request[AnyRef]): SimpleResult = {
    Ok(views.html.merge_gg_accounts(PortalLink(buildPortalUrl("servicesDeEnrolment")), PortalLink(buildPortalUrl("otherServicesEnrolment"))))
  }

}
