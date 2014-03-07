package controllers.bt

import controllers.common._
import uk.gov.hmrc.common.PortalUrlBuilder
import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.{SimpleResult, Request}
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import controllers.common.service.Connectors
import controllers.common.actions.Actions
import views.helpers.Link
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
    Ok(views.html.merge_gg_accounts(
        Link.toPortalPage(url = buildPortalUrl("servicesDeEnrolment") , id = Some("deEnrolHref"), value = Some("Remove the tax from the account(s) you don't want to use anymore")),
        Link.toPortalPage(url = buildPortalUrl("otherServicesEnrolment"), id = Some("enrolHref"), value = Some(Messages("bt.mergeggaccount.step4.linkText")))
    ))
  }

}
