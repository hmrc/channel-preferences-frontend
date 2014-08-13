package controllers.sa.prefs.external

import play.api.mvc._
import uk.gov.hmrc.emailaddress.EmailAddress
import concurrent.Future
import controllers.common.service.{Connectors, FrontEndConfig}
import controllers.common.BaseController
import connectors.EmailConnector
import com.netaporter.uri.dsl._
import play.Logger
import com.netaporter.uri.Uri
import controllers.sa.prefs._
import uk.gov.hmrc.common.crypto.Encrypted
import play.api.mvc.SimpleResult
import connectors.{SaPreference, SaEmailPreference, PreferencesConnector}

class SaPrefsController(whiteList: Set[String], preferencesConnector: PreferencesConnector, emailConnector: EmailConnector) extends BaseController with PreferencesControllerHelper {

  implicit val wl = whiteList

  def this() = this(FrontEndConfig.redirectDomainWhiteList, PreferencesConnector, EmailConnector)

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
                    Logger.debug(s"Redirecting $utr back to $returnUrl as we have no preference for them")
                    Redirect(returnUrl)
                }
            }
        }
    }
}