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
  with PortalUrlBuilder
  with BusinessTaxRegimeRoots {

  def this() = this(new AccountSummariesFactory(), Connectors.preferencesConnector, Connectors.auditConnector)(Connectors.authConnector)

  def home = AuthenticatedBy(GovernmentGateway).async {
    user => request => renderHomePage(user, request)
  }

  def homeFromLogin = AuthenticatedBy(GovernmentGateway).async {
    user => request => businessTaxHomepageFromLogin(user, request)
  }

  private[bt] def businessTaxHomepageFromLogin(implicit user: User, request: Request[AnyRef]): Future[SimpleResult] = {
    user.regimes.sa.map(_ => capturePrintPreferences(user.getSa.utr)).getOrElse(renderHomePage)
  }

  private[bt] def renderHomePage(implicit user: User, request: Request[AnyRef]): Future[SimpleResult] = {
    val accountSummariesF = accountSummaryFactory.create(buildPortalUrl)
    val otherServicesLink = LinkMessage.internalLink(controllers.bt.routes.OtherServicesController.otherServices().url, "link")
    val mergeGGAccountsLink = LinkMessage.internalLink(controllers.bt.routes.MergeGGAccountsController.mergeGGAccounts().url, Messages("bt.home.mergeAccountsLink"))
    accountSummariesF.map {
      accountSummaries =>
        Ok(views.html.business_tax_home(accountSummaries, otherServicesLink, mergeGGAccountsLink))
    }
  }

  private def capturePrintPreferences(utr: SaUtr)(implicit user: User, request: Request[AnyRef]) = {
    preferencesConnector.getPreferences(utr).flatMap {
      preferences =>
          preferences.map(_ => renderHomePage).getOrElse(Future.successful(Redirect(PreferencesRoutes.SaPrefsController.displayPrefsForm(None))))
    }
  }


}
