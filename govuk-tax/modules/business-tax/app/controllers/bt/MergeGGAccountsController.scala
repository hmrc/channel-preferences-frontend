package controllers.bt

import controllers.common._
import uk.gov.hmrc.common.PortalUrlBuilder
import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.{SimpleResult, Request}
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import controllers.common.service.Connectors
import controllers.common.actions.Actions
import views.helpers.LinkMessage
import play.api.i18n.Messages

class MergeGGAccountsController(override val auditConnector: AuditConnector)(implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with PortalUrlBuilder {

  def this() = this(Connectors.auditConnector)(Connectors.authConnector)

  def mergeGGAccounts = AuthenticatedBy(GovernmentGateway) {
    user => request => mergeGGAccountsPage(user, request)
  }

  private[bt] def mergeGGAccountsPage(implicit user: User, request: Request[AnyRef]): SimpleResult = {
    val deEnrolServiceLink = LinkMessage.portalLink(buildPortalUrl("servicesDeEnrolment"), Some("Remove the tax from the account you don't want to use anymore"), Some("deEnrolHref")) //TODO find a way to send message containing '
    val enrolServiceLink = LinkMessage.portalLink(buildPortalUrl("otherServicesEnrolment"), Some(Messages("bt.mergeggaccount.step4.linkText")), Some("enrolHref"))
    Ok(views.html.merge_gg_accounts(deEnrolServiceLink, enrolServiceLink))
  }

}
