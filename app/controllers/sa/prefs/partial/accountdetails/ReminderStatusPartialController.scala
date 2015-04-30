package controllers.sa.prefs.partial.accountdetails

import connectors.PreferencesConnector
import controllers.sa.prefs.SaRegimeWithoutRedirection
import controllers.sa.prefs.config.Global
import controllers.sa.prefs.internal.PreferencesControllerHelper
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

object ReminderStatusPartialController extends ReminderStatusPartialController {
  lazy val auditConnector = Global.auditConnector
  lazy val authConnector = Global.authConnector
  lazy val preferencesConnector = PreferencesConnector
}


trait ReminderStatusPartialController
  extends FrontendController
  with Actions
  with ReminderStatusPartialHtml
  with PreferencesControllerHelper {

  def emailRemindersStatus() = AuthorisedFor(taxRegime = SaRegimeWithoutRedirection, redirectToOrigin = false).async {
    authContext => implicit request => detailsStatus(authContext.principal.accounts.sa.get.utr).map(Ok(_))
  }
}
