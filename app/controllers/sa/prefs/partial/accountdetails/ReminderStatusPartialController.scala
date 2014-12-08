package controllers.sa.prefs.partial.accountdetails

import connectors.PreferencesConnector
import controllers.common.BaseController
import controllers.common.actions.Actions
import controllers.common.service.Connectors
import controllers.sa.prefs.SaRegimeWithoutRedirection
import controllers.sa.prefs.internal.PreferencesControllerHelper
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector

class ReminderStatusPartialController(val auditConnector: AuditConnector,
                                      val preferencesConnector: PreferencesConnector)(implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with ReminderStatusPartialHtml
  with PreferencesControllerHelper {

  def this() = this(Connectors.auditConnector, PreferencesConnector)(Connectors.authConnector)

  def emailRemindersStatus() = AuthorisedFor(regime = SaRegimeWithoutRedirection, redirectToOrigin = false).async {
    user => implicit request => detailsStatus(user.userAuthority.accounts.sa.get.utr).map(Ok(_))
  }
}
