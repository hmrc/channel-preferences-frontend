package controllers.bt

import controllers.common.{GovernmentGateway, BaseController}
import controllers.common.actions.{HeaderCarrier, Actions}
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.preferences.{SaPreference, PreferencesConnector}
import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.{SimpleResult, Request}
import scala.concurrent.Future
import uk.gov.hmrc.common.microservice.email.EmailConnector
import uk.gov.hmrc.common.microservice.sa.domain.SaRegime

class AccountDetailsController(override val auditConnector: AuditConnector, val preferencesConnector: PreferencesConnector,
                                val emailConnector: EmailConnector)(implicit override val authConnector: AuthConnector) extends BaseController
with Actions
with BusinessTaxRegimeRoots
with EmailControllerHelper {

  def this() = this(Connectors.auditConnector, Connectors.preferencesConnector, Connectors.emailConnector)(Connectors.authConnector)


  def accountDetails() = AuthenticatedBy(GovernmentGateway).async {
    user => request => accountDetailsPage(user, request)
  }

  def changeEmailAddress() =  AuthorisedFor(account = SaRegime).async {
    user => request => changeEmailAddressPage(user, request)
  }

  def submitEmailAddress = TODO


  private[bt] def accountDetailsPage(implicit user: User, request: Request[AnyRef]) = {
    val saPreferenceF = user.regimes.sa.map(regime => preferencesConnector.getPreferences(regime.utr)(HeaderCarrier(request))).getOrElse(Future.successful(None))

    for {
      preference <- saPreferenceF
    } yield Ok(views.html.account_details(preference))
    
  }

  private[bt] def changeEmailAddressPage(implicit user: User, request: Request[AnyRef]): Future[SimpleResult] = {
    preferencesConnector.getPreferences(user.getSa.utr).map {
      response => response match {
        case Some(prefs) => Ok(views.html.account_details_update_email_address(prefs.email.get, emailForm))
        case None => BadRequest("Cannot find existing preferences to update")
      }
    }
  }
}
