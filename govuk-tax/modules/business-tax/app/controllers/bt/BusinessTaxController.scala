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

class BusinessTaxController(accountSummaryFactory: AccountSummariesFactory,
                            preferencesConnector: PreferencesConnector,
                            override val auditConnector: AuditConnector)
                           (implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with PortalUrlBuilder {

  def this() = this(new AccountSummariesFactory(), Connectors.preferencesConnector, Connectors.auditConnector)(Connectors.authConnector)

  def home(fromLogin: Option[String]) = AuthenticatedBy(GovernmentGateway).async {
    user => request => businessTaxHomepage(fromLogin)(user, request)
  }

  private[bt] def businessTaxHomepage(fromLogin: Option[String])(implicit user: User, request: Request[AnyRef]): Future[SimpleResult] = {
    fromLogin match {
      case Some("true") => if (user.regimes.sa.isDefined) capturePrintPreferences(user.getSa.utr) else renderHomePage
      case _ => renderHomePage
    }
  }

  private def renderHomePage(implicit user: User, request: Request[AnyRef]): Future[SimpleResult] = {
    implicit val headerCarrier = HeaderCarrier(request)
    val accountSummariesF = accountSummaryFactory.create(buildPortalUrl)
    val otherServicesLink = LinkMessage.internalLink(controllers.bt.routes.OtherServicesController.otherServices().url, "link")
    val enrolServiceLink = LinkMessage.portalLink(buildPortalUrl("otherServicesEnrolment"))
    val removeServiceLink = LinkMessage.portalLink(buildPortalUrl("servicesDeEnrolment"))

    accountSummariesF.map { accountSummaries =>
      Ok(views.html.business_tax_home(accountSummaries, otherServicesLink, enrolServiceLink, removeServiceLink))
    }
  }

  private def capturePrintPreferences(utr: SaUtr)(implicit user: User, request: Request[AnyRef]) = {

    preferencesConnector.getPreferences(utr) match {
      case Some(pref) => renderHomePage
      case None => Future.successful(Redirect(PreferencesRoutes.SaPrefsController.displayPrefsForm(None)))
    }
  }


}
