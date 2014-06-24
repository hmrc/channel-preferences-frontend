package controllers.sa.prefs.external

import play.api.mvc._
import uk.gov.hmrc.emailaddress.EmailAddress
import concurrent.Future
import controllers.common.service.{Connectors, FrontEndConfig}
import controllers.common.BaseController
import uk.gov.hmrc.common.microservice.email.EmailConnector
import uk.gov.hmrc.common.microservice.preferences.{SaEmailPreference, PreferencesConnector}
import com.netaporter.uri.dsl._
import play.Logger
import com.netaporter.uri.Uri
import controllers.sa.prefs._
import uk.gov.hmrc.common.crypto.Encrypted
import uk.gov.hmrc.common.microservice.preferences.SaPreference
import play.api.mvc.SimpleResult

class SaPrefsController(whiteList: Set[String], preferencesConnector: PreferencesConnector, emailConnector: EmailConnector) extends BaseController with PreferencesControllerHelper {

  implicit val wl = whiteList

  def this() = this(FrontEndConfig.redirectDomainWhiteList, Connectors.preferencesConnector, Connectors.emailConnector)

  def index(encryptedToken: String, encodedReturnUrl: String, emailAddressToPrefill: Option[Encrypted[EmailAddress]]) =
    DecodeAndWhitelist(encodedReturnUrl) {
      returnUrl =>
        DecryptAndValidate(encryptedToken, returnUrl) {
          token =>
            Action.async {
              implicit request =>
                val utr = token.utr
                preferencesConnector.getPreferencesUnsecured(utr) map {
                  case Some(SaPreference(_, Some(SaEmailPreference(emailAddress, _, _, _)))) =>
                    Logger.debug(s"Redirecting $utr back to $returnUrl as they have opted-in")
                    Redirect(returnUrl ? ("email" -> SsoPayloadCrypto.encrypt(emailAddress)))

                  case Some(SaPreference(_, None)) =>
                    Logger.debug(s"Redirecting $utr back to $returnUrl as they have opted-out")
                    Redirect(returnUrl)

                  case None =>
                    Logger.debug(s"Requesting preferences from $utr as they have none set")
                    displayPreferencesFormAction(emailAddressToPrefill.map(_.decryptedValue), getSavePrefsCall(token, returnUrl))
                }
            }
        }
    }

  def confirm(encryptedToken: String, encodedReturnUrl: String) =
    DecodeAndWhitelist(encodedReturnUrl) {
      returnUrl =>
        DecryptAndValidate(encryptedToken, returnUrl) {
          token =>
            Action.async {
              implicit request =>
                preferencesConnector.getPreferencesUnsecured(token.utr).map {
                  case Some(SaPreference(true, Some(SaEmailPreference(emailAddress, _, _, _)))) =>
                    Ok(views.html.sa.prefs.sa_printing_preference_confirm(user = None, redirectUrl = returnUrl ? ("email" -> SsoPayloadCrypto.encrypt(emailAddress))))
                  case _ => PreconditionFailed
                }
            }
        }
    }

  def noAction(encodedReturnUrl: String, digital: Boolean) =
    DecodeAndWhitelist(encodedReturnUrl) {
      returnUrl =>
        Action {
          Ok(views.html.sa.prefs.sa_printing_preference_no_action(returnUrl, digital))
        }
    }

  def submitPrefsForm(encryptedToken: String, encodedReturnUrl: String) =
    DecodeAndWhitelist(encodedReturnUrl) {
      returnUrl =>
        DecryptAndValidate(encryptedToken, returnUrl) {
          token =>
            Action.async {
              implicit request =>
                saveEmailPreferences(token, returnUrl)
            }
        }
    }

  private def getSavePrefsCall(token: Token, returnUrl: Uri) = controllers.sa.prefs.external.routes.SaPrefsController.submitPrefsForm(token.encryptedToken, returnUrl)

  def saveEmailPreferences(token: Token, returnUrl: Uri)(implicit request: Request[AnyContent]): Future[SimpleResult] = {
    preferencesConnector.getPreferencesUnsecured(token.utr).flatMap {
      case Some(saPreference) =>
        Future.successful(Redirect(routes.SaPrefsController.noAction(returnUrl, saPreference.digital)))
      case None =>
        submitPreferencesForm(
          errorsView = getSubmitPreferencesView(getSavePrefsCall(token, returnUrl)),
          emailWarningView = emailAddr => views.html.sa.prefs.sa_printing_preference_warning_email(EmailAddress(emailAddr), token, returnUrl),
          emailConnector = emailConnector,
          saUtr = token.utr,
          savePreferences = (utr, digital, email, hc) =>
            preferencesConnector.savePreferencesUnsecured(utr, digital, email)(hc).map(_ =>
              digital match {
                case true => Redirect(routes.SaPrefsController.confirm(token.encryptedToken, returnUrl))
                case false => Redirect(returnUrl)
              }
            )(mdcExecutionContext(hc))
        )
    }
  }
}