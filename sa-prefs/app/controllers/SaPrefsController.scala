package controllers

import play.api.data._
import play.api.mvc.Results._
import play.mvc._
import play.api.data.Forms._
import play.api.mvc.{AnyContent, Request, Action}
import uk.gov.hmrc.{EmailConnector, TokenExpiredException, PreferencesConnector, TokenEncryption}
import play.api.{Logger, Play}
import java.net.URLDecoder
import controllers.service.{RedirectWhiteListService, FrontEndConfig}
import concurrent.Future

class SaPrefsController extends Controller {

  implicit lazy val preferencesConnector = new PreferencesConnector()
  implicit lazy val emailConnector = new EmailConnector()

  private[controllers] val redirectWhiteListService = new RedirectWhiteListService(FrontEndConfig.redirectDomainWhiteList)

  object WithValidReturnUrl {
    def apply(return_url: String)(action: (Action[AnyContent])) =
      Action.async {
        request: Request[AnyContent] =>
          redirectWhiteListService.check(return_url) match {
            case true => action(request)
            case false => Future.successful(InternalServerError)
          }
      }
  }

  object WithValidToken {
    def apply(token: String, return_url: String)(action: String => (Action[AnyContent])) =
      Action.async {
        request: Request[AnyContent] =>
          try {
            implicit val utr = SsoPayloadEncryptor.decryptToken(token, FrontEndConfig.tokenTimeout)
            action(utr)(request)
          } catch {
            case e: TokenExpiredException => {
              Logger.error("Unable to validate token", e)
              Future.successful(Redirect(return_url))
            }
            case e: Exception => {
              Logger.error("Exception happened while decrypting the token", e)
              Future.successful(Redirect(return_url))
            }
          }
      }
  }

  def index(token: String, return_url: String, emailAddress: Option[String]) = WithValidReturnUrl(return_url)(WithValidToken(token, return_url)(
    utr =>
      Action {
        implicit request =>
          preferencesConnector.getPreferences(utr) match {
            case Some(saPreference) => Redirect(return_url)
            case _ => Ok(views.html.sa_printing_preference(emailForm.fill(EmailPreferenceData((emailAddress.getOrElse(""), emailAddress), None)), token, return_url))
          }
      })
  )

  def confirm(return_url: String) = Action {
    Ok(views.html.sa_printing_preference_confirm(return_url))
  }

  def noAction(return_url: String, digital: Boolean) = Action {
    Ok(views.html.sa_printing_preference_no_action(return_url, digital))
  }

  private val emailForm: Form[EmailPreferenceData] = Form[EmailPreferenceData](mapping(
    "email" -> tuple(
      "main" -> email,
      "confirm" -> optional(text)
    ).verifying(
      "email.confirmation.emails.unequal", email => email._1 == email._2.getOrElse("")
    ),
    "emailVerified" -> optional(text)
  )(EmailPreferenceData.apply)(EmailPreferenceData.unapply))

  //  val emailForm: Form[String] = Form[String](single("email" -> email))

  def submitPrefsForm(token: String, return_url: String) = WithValidReturnUrl(return_url) {
    WithValidToken(token, return_url) {
      utr =>
        Action {
          request =>
            emailForm.bindFromRequest()(request).fold(
              errors => BadRequest(views.html.sa_printing_preference(errors, token, return_url)),
              emailForm => {
                if (emailForm.isEmailVerified || emailConnector.validateEmailAddress(emailForm.mainEmail)) {
                  preferencesConnector.getPreferences(utr) match {
                    case Some(saPreference) => Redirect(routes.SaPrefsController.noAction(return_url, saPreference.digital))
                    case None => {
                      preferencesConnector.savePreferences(utr, true, Some(emailForm.mainEmail))
                      //Play redirect is encoding query params, we need to decode the url to avoid double encoding
                      Redirect(routes.SaPrefsController.confirm(URLDecoder.decode(return_url, "UTF-8")))
                    }
                  }
                }
                else Ok(views.html.sa_printing_preference_warning_email(emailForm.mainEmail, token, return_url))
              }
            )
        }
    }
  }


  def submitKeepPaperForm(token: String, return_url: String) = WithValidReturnUrl(return_url)(WithValidToken(token, return_url)(
    utr =>
      Action {
        request =>
          preferencesConnector.getPreferences(utr) match {
            case Some(saPreference) => Redirect(routes.SaPrefsController.noAction(return_url, saPreference.digital))
            case None => {
              preferencesConnector.savePreferences(utr, false)
              Redirect(return_url)
            }
          }
      })
  )

}

object SsoPayloadEncryptor extends TokenEncryption {
  val encryptionKey = Play.current.configuration.getString("sso.encryption.key").get
}

case class EmailPreferenceData(email: (String, Option[String]), emailVerified: Option[String]) {
  lazy val isEmailVerified = emailVerified == Some("true")

  def mainEmail = email._1
}