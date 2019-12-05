package controllers.filing

import java.util.Collections

import com.netaporter.uri.config.UriConfig
import com.netaporter.uri.dsl._
import com.netaporter.uri.encoding._
import connectors.EntityResolverConnector
import model.Encrypted
import play.api.Mode.Mode
import play.api.Play.current
import play.api.mvc._
import play.api.{Configuration, Play}
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.collection.JavaConversions._

class FilingInterceptController(whiteList: Set[String], preferencesConnector: EntityResolverConnector) extends FrontendController {

  implicit val wl: Set[String] = whiteList
  implicit val config = UriConfig(encoder = percentEncode)

  def this() = this(FilingInterceptController.redirectDomainWhiteList, EntityResolverConnector)

  def redirectWithEmailAddress(encryptedToken: String, encodedReturnUrl: String, emailAddressToPrefill: Option[Encrypted[EmailAddress]]) =
    DecodeAndWhitelist(encodedReturnUrl) { returnUrl =>
      DecryptAndValidate(encryptedToken, returnUrl) { token =>
        Action.async { implicit request =>
          val utr = token.utr
          preferencesConnector.getEmailAddress(utr) map {
            case Some(emailAddress) =>
              Redirect(returnUrl ? ("email" -> TokenEncryption.encrypt(PlainText(emailAddress)).value))
            case _ =>
              Redirect(returnUrl)
          }
        }
      }
    }
}

object FilingInterceptController extends RunMode {
  lazy val redirectDomainWhiteList = Play.configuration.getStringList(s"govuk-tax.$env.portal.redirectDomainWhiteList").getOrElse(Collections.emptyList()).toSet

  override protected def mode: Mode = Play.current.mode

  override protected def runModeConfiguration: Configuration = Play.current.configuration
}
