package controllers.sa.prefs.partial.accountdetails

import connectors.PreferencesConnector
import controllers.common.BaseController
import controllers.sa.prefs.SaRegimeWithoutRedirection
import controllers.sa.prefs.internal.PreferencesControllerHelper
import uk.gov.hmrc.play.auth.frontend.connectors.AuthConnector
import uk.gov.hmrc.play.config.AuditConnector
import uk.gov.hmrc.play.frontend.auth.Actions

object ReminderStatusPartialController extends ReminderStatusPartialController {
  lazy val auditConnector = AuditConnector
  lazy val authConnector = AuthConnector
  lazy val preferencesConnector = PreferencesConnector
}


trait ReminderStatusPartialController
  extends BaseController
  with Actions
  with ReminderStatusPartialHtml
  with PreferencesControllerHelper {

  def emailRemindersStatus() = AuthorisedFor(regime = SaRegimeWithoutRedirection, redirectToOrigin = false).async {
    user => implicit request => detailsStatus(user.userAuthority.accounts.sa.get.utr).map(Ok(_))
  }
}
