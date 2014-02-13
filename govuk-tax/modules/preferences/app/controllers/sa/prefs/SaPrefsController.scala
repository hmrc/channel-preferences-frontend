package controllers.sa.prefs

import play.api.mvc.{Request, AnyContent, Action}
import concurrent.Future
import controllers.common.service.{Connectors, FrontEndConfig}
import controllers.common.BaseController
import uk.gov.hmrc.common.microservice.email.EmailConnector
import uk.gov.hmrc.common.microservice.preferences.{SaEmailPreference, PreferencesConnector}
import com.netaporter.uri.dsl._
import uk.gov.hmrc.domain.Email
import play.Logger
import controllers.common.preferences.PreferencesControllerHelper
import controllers.common.actions.HeaderCarrier
import com.netaporter.uri.Uri
import controllers.common.preferences.service.SsoPayloadCrypto
import uk.gov.hmrc.common.microservice.preferences.SaPreference
import controllers.common.preferences.service.Token
import play.api.mvc.SimpleResult
import uk.gov.hmrc.domain.SaUtr

class SaPrefsController(whiteList: Set[String], preferencesConnector: PreferencesConnector, emailConnector: EmailConnector) extends BaseController with PreferencesControllerHelper {

  implicit val wl = whiteList

  def this() = this(FrontEndConfig.redirectDomainWhiteList, Connectors.preferencesConnector, Connectors.emailConnector)

  def index(encryptedToken: String, encodedReturnUrl: String, emailAddressToPrefill: Option[Email]) =
    DecodeAndWhitelist(encodedReturnUrl) {
      returnUrl =>
        DecryptAndValidate(encryptedToken, returnUrl) {
          token =>
            Action.async {
              implicit request =>
                val utr = token.utr
                preferencesConnector.getPreferencesUnsecured(utr) map {
                  case Some(SaPreference(_, Some(SaEmailPreference(emailAddress, _, _)))) =>
                    Logger.debug(s"Redirecting $utr back to $returnUrl as they have opted-in")
                    Redirect(returnUrl ? ("emailAddress" -> SsoPayloadCrypto.encrypt(emailAddress)))

                  case Some(SaPreference(_, None)) =>
                    Logger.debug(s"Redirecting $utr back to $returnUrl as they have opted-out")
                    Redirect(returnUrl)

                  case None =>
                    Logger.debug(s"Requesting preferences from $utr as they have none set")
                    displayPreferencesForm(emailAddressToPrefill, getSavePrefsCall(token, returnUrl), getKeepPaperCall(token, returnUrl))
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
                  case Some(SaPreference(true, Some(SaEmailPreference(emailAddress, _, _)))) =>
                    Ok(views.html.preferences.sa_printing_preference_confirm(user = None, redirectUrl = returnUrl ? ("emailAddress" -> SsoPayloadCrypto.encrypt(emailAddress))))
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

  private def savePreferences(utr: SaUtr, digital: Boolean, email: Option[String] = None, hc: HeaderCarrier) = {
    preferencesConnector.savePreferencesUnsecured(utr, digital, email)(hc)
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

  private def getSavePrefsCall(token: Token, returnUrl: Uri) = controllers.sa.prefs.routes.SaPrefsController.submitPrefsForm(token.encryptedToken, returnUrl)

  private def getKeepPaperCall(token: Token, returnUrl: Uri) = controllers.sa.prefs.routes.SaPrefsController.submitKeepPaperForm(token.encryptedToken, returnUrl)

  def saveEmailPreferences(token: Token, returnUrl: Uri)(implicit request: Request[AnyContent]): Future[SimpleResult] = {
    preferencesConnector.getPreferencesUnsecured(token.utr).flatMap {
      case Some(saPreference) =>
        Future.successful(Redirect(routes.SaPrefsController.noAction(returnUrl, saPreference.digital)))
      case None =>
        submitPreferencesForm(
          errorsView = getSubmitPreferencesView(getSavePrefsCall(token, returnUrl), getKeepPaperCall(token, returnUrl)),
          emailWarningView = views.html.sa.prefs.sa_printing_preference_warning_email(_, token, returnUrl),
          successRedirect = () => routes.SaPrefsController.confirm(token.encryptedToken, returnUrl),
          emailConnector = emailConnector,
          saUtr = token.utr,
          savePreferences = savePreferences
        )
    }
  }

  def submitKeepPaperForm(encryptedToken: String, encodedReturnUrl: String) =
    DecodeAndWhitelist(encodedReturnUrl) {
      returnUrl =>
        DecryptAndValidate(encryptedToken, returnUrl) {
          token =>
            Action.async {
              implicit request =>
                preferencesConnector.getPreferencesUnsecured(token.utr) flatMap {
                  case Some(saPreference) =>
                    Future.successful(Redirect(routes.SaPrefsController.noAction(returnUrl, saPreference.digital)))
                  case None =>
                    preferencesConnector.savePreferencesUnsecured(token.utr, digital = false, None).map(_ =>
                      Redirect(returnUrl)
                    )
                }
            }
        }
    }
}