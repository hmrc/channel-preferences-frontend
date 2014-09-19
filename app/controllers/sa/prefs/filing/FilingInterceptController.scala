package controllers.sa.prefs.filing

import com.netaporter.uri.config.UriConfig
import com.netaporter.uri.dsl._
import com.netaporter.uri.encoding._
import connectors.{EmailConnector, PreferencesConnector}
import controllers.common.BaseController
import controllers.common.service.FrontEndConfig
import play.api.mvc._
import uk.gov.hmrc.crypto.Encrypted
import uk.gov.hmrc.emailaddress.EmailAddress

class FilingInterceptController(whiteList: Set[String], preferencesConnector: PreferencesConnector, emailConnector: EmailConnector) extends BaseController {

  implicit val wl = whiteList
  implicit val config = UriConfig(encoder = percentEncode)

  def this() = this(FrontEndConfig.redirectDomainWhiteList, PreferencesConnector, EmailConnector)

  def redirectWithEmailAddress(encryptedToken: String, encodedReturnUrl: String, emailAddressToPrefill: Option[Encrypted[EmailAddress]]) =
    DecodeAndWhitelist(encodedReturnUrl) { returnUrl =>
      DecryptAndValidate(encryptedToken, returnUrl) { token =>
        Action.async { implicit request =>
          val utr = token.utr
          preferencesConnector.getEmailAddress(utr) map {
            case Some(emailAddress) =>
              Redirect(returnUrl ? ("email" -> TokenEncryption.encrypt(emailAddress)))
            case _ =>
              Redirect(returnUrl)
          }
        }
      }
    }
}

