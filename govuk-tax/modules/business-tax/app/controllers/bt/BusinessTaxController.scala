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
import controllers.common.actions.{HeaderCarrier, Actions}
import scala.concurrent._
import ExecutionContext.Implicits.global
import uk.gov.hmrc.common.microservice.preferences.PreferencesConnector
import uk.gov.hmrc.domain.SaUtr
import controllers.bt.prefs.{routes => PreferencesRoutes}
import play.api.i18n.Messages

class BusinessTaxController(accountSummaryFactory: AccountSummariesFactory,
                            preferencesConnector: PreferencesConnector,
                            override val auditConnector: AuditConnector)
                           (implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with PortalUrlBuilder {

  def this() = this(new AccountSummariesFactory(), Connectors.preferencesConnector, Connectors.auditConnector)(Connectors.authConnector)

  def home(fromLogin: Option[String]) = AuthenticatedBy(GovernmentGateway).async {
    user => request => businessTaxHomepage(fromLogin.exists(_ == "true"))(user, request)
  }

  private[bt] def businessTaxHomepage(fromLogin: Boolean)(implicit user: User, request: Request[AnyRef]): Future[SimpleResult] = {
    if (fromLogin && user.regimes.sa.isDefined) capturePrintPreferences(user.getSa.utr) else renderHomePage
  }

  private def renderHomePage(implicit user: User, request: Request[AnyRef]): Future[SimpleResult] = {
    val accountSummariesF = accountSummaryFactory.create(buildPortalUrl)
    val otherServicesLink = LinkMessage.internalLink(controllers.bt.routes.OtherServicesController.otherServices().url, "link")
    val mergeGGAccountsLink = LinkMessage.internalLink(controllers.bt.routes.MergeGGAccountsController.mergeGGAccounts().url, Messages("bt.home.mergeAccountsLink"))
    accountSummariesF.map { accountSummaries =>
      Ok(views.html.business_tax_home(accountSummaries, otherServicesLink, mergeGGAccountsLink))
    }
  }

  private def capturePrintPreferences(utr: SaUtr)(implicit user: User, request: Request[AnyRef]) = {
    preferencesConnector.getPreferences(utr) match {
      case Some(pref) => renderHomePage
      case None => Future.successful(Redirect(PreferencesRoutes.SaPrefsController.displayPrefsForm(None)))
    }
  }


}
