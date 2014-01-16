package controllers.sa.prefs

import play.api.data._
import play.api.data.Forms._
import play.api.mvc.{AnyContent, Request, Action}
import play.api.Logger
import java.net.URLDecoder
import concurrent.Future
import controllers.common.service.FrontEndConfig
import controllers.sa.prefs.service.{SsoPayloadCrypto, TokenExpiredException, RedirectWhiteListService}
import controllers.common.BaseController
import scala.Some
import uk.gov.hmrc.common.microservice.email.EmailConnector
import uk.gov.hmrc.common.microservice.preferences.PreferencesConnector

class SaPrefsController extends BaseController {

  implicit lazy val preferencesConnector = new PreferencesConnector()
  implicit lazy val emailConnector = new EmailConnector()

  private[controllers] val redirectWhiteListService = new RedirectWhiteListService(FrontEndConfig.redirectDomainWhiteList)

  object WithValidReturnUrl {
    def apply(return_url: String)(action: (Action[AnyContent])) =
      Action.async {
        request: Request[AnyContent] =>
          redirectWhiteListService.check(return_url) match {
            case true => action(request)
            case false =>
              Logger.debug(s"Return URL '$return_url' was invalid as it was not on the whitelist")
              Future.successful(InternalServerError)
          }
      }
  }

  object WithValidToken {
    def apply(token: String, return_url: String)(action: String => (Action[AnyContent])) =
      Action.async {
        request: Request[AnyContent] =>
          try {
            implicit val utr = SsoPayloadCrypto.decryptToken(token, FrontEndConfig.tokenTimeout)
            action(utr)(request)
          } catch {
            case e: TokenExpiredException =>
              Logger.error("Unable to validate token", e)
              Future.successful(Redirect(return_url))
            case e: Exception =>
              Logger.error("Exception happened while decrypting the token", e)
              Future.successful(Redirect(return_url))
          }
      }
  }

  def index(token: String, return_url: String, emailAddress: Option[String]) =
    WithValidReturnUrl(return_url) {
      WithValidToken(token, return_url) {
        utr =>
          Action.async {
            implicit request =>
              preferencesConnector.getPreferencesUnsecured(utr) map {
                case Some(saPreference) =>
                  Redirect(return_url)
                case _ =>
                  Ok(
                    views.html.sa.prefs.sa_printing_preference(
                      emailForm.fill(EmailPreferenceData((emailAddress.getOrElse(""), emailAddress), None)),
                      token,
                      return_url))
              }
          }
      }
    }

  def confirm(return_url: String) = Action {
    Ok(views.html.sa.prefs.sa_printing_preference_confirm(return_url))
  }

  def noAction(return_url: String, digital: Boolean) = Action {
    Ok(views.html.sa.prefs.sa_printing_preference_no_action(return_url, digital))
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

  def submitPrefsForm(token: String, return_url: String) =
    WithValidReturnUrl(return_url) {
      WithValidToken(token, return_url) {
        utr =>
          Action.async {
            implicit request =>
              emailForm.bindFromRequest()(request).fold(
                errors => Future.successful(BadRequest(views.html.sa.prefs.sa_printing_preference(errors, token, return_url))),
                emailForm => {
                  val isEmailValid =
                    if (emailForm.isEmailVerified) Future.successful(true)
                    else emailConnector.validateEmailAddress(emailForm.mainEmail)
                  isEmailValid flatMap {
                    case true => {
                      preferencesConnector.getPreferencesUnsecured(utr).map {
                        case Some(saPreference) =>
                          Redirect(routes.SaPrefsController.noAction(return_url, saPreference.digital))
                        case None => {
                          // FIXME this should map over the result
                          preferencesConnector.savePreferencesUnsecured(utr, true, Some(emailForm.mainEmail))
                          //                  Play redirect is encoding query params, we need to decode the url to avoid double encoding
                          Redirect(routes.SaPrefsController.confirm(URLDecoder.decode(return_url, "UTF-8")))
                        }
                      }
                    }
                    case false => Future.successful(Ok(views.html.sa.prefs.sa_printing_preference_warning_email(emailForm.mainEmail, token, return_url)))
                  }
                }
              )
          }
      }
    }


  def submitKeepPaperForm(token: String, return_url: String) =
    WithValidReturnUrl(return_url) {
      WithValidToken(token, return_url) {
        utr =>
          Action.async {
            implicit request =>
              preferencesConnector.getPreferencesUnsecured(utr) map {
                case Some(saPreference) =>
                  Redirect(routes.SaPrefsController.noAction(return_url, saPreference.digital))
                case None =>
                  preferencesConnector.savePreferencesUnsecured(utr, false)
                  Redirect(return_url)
              }
          }
      }
    }
}

case class EmailPreferenceData(email: (String, Option[String]), emailVerified: Option[String]) {
  lazy val isEmailVerified = emailVerified == Some("true")

  def mainEmail = email._1
}