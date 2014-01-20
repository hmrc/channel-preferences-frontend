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
import uk.gov.hmrc.common.microservice.preferences.{SaEmailPreference, SaPreference, PreferencesConnector}
import com.netaporter.uri.dsl._
import com.netaporter.uri.Uri

class SaPrefsController extends BaseController {

  implicit lazy val preferencesConnector = new PreferencesConnector()
  implicit lazy val emailConnector = new EmailConnector()

  private[controllers] val redirectWhiteListService = new RedirectWhiteListService(FrontEndConfig.redirectDomainWhiteList)

  object ValidateAndDecode {
    def apply(encodedReturnUrl: String)(action: (Uri => Action[AnyContent])) =
      Action.async {
        request: Request[AnyContent] =>
          redirectWhiteListService.check(encodedReturnUrl) match {
            case true => action(URLDecoder.decode(encodedReturnUrl, "UTF-8"))(request)
            case false =>
              Logger.debug(s"Return URL '$encodedReturnUrl' was invalid as it was not on the whitelist")
              Future.successful(InternalServerError) // FIXME This should be a bad request
          }
      }
  }

  object WithValidToken {
    def apply(token: String, returnUrl: Uri)(action: String => (Action[AnyContent])) =
      Action.async {
        request: Request[AnyContent] =>
          try {
            implicit val utr = SsoPayloadCrypto.decryptToken(token, FrontEndConfig.tokenTimeout)
            action(utr)(request)
          } catch {
            case e: TokenExpiredException =>
              Logger.error("Unable to validate token", e)
              Future.successful(Redirect(returnUrl))
            case e: Exception =>
              Logger.error("Exception happened while decrypting the token", e)
              Future.successful(Redirect(returnUrl))
          }
      }
  }

  def index(token: String, encodedReturnUrl: String, emailAddress: Option[String]) =
    ValidateAndDecode(encodedReturnUrl) { returnUrl =>
      WithValidToken(token, returnUrl) {
        utr =>
          Action.async {
            implicit request =>
              preferencesConnector.getPreferencesUnsecured(utr) map {
                case Some(saPreference) =>
                  Redirect(returnUrl)
                case _ =>
                  Ok(
                    views.html.sa.prefs.sa_printing_preference(
                      emailForm.fill(EmailPreferenceData((emailAddress.getOrElse(""), emailAddress), None)),
                      token,
                      returnUrl))
              }
          }
      }
    }

  def confirm(token: String, encodedReturnUrl: String) =
    ValidateAndDecode(encodedReturnUrl) { returnUrl =>
      WithValidToken(token, returnUrl) {
        utr => Action.async { implicit request =>
          preferencesConnector.getPreferencesUnsecured(utr).map {
            case Some(SaPreference(true, Some(SaEmailPreference(email, _, _)))) =>
              Ok(views.html.sa.prefs.sa_printing_preference_confirm(returnUrl ? ("emailAddress" -> SsoPayloadCrypto.encrypt(email))))
            case _ => PreconditionFailed
          }
        }
      }
    }

  def noAction(encodedReturnUrl: String, digital: Boolean) =
    ValidateAndDecode(encodedReturnUrl) { returnUrl =>
      Action {
        Ok(views.html.sa.prefs.sa_printing_preference_no_action(returnUrl, digital))
      }
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

  def submitPrefsForm(token: String, encodedReturnUrl: String) =
    ValidateAndDecode(encodedReturnUrl) { returnUrl =>
      WithValidToken(token, returnUrl) {
        utr =>
          Action.async {
            implicit request =>
              emailForm.bindFromRequest()(request).fold(
                errors => Future.successful(BadRequest(views.html.sa.prefs.sa_printing_preference(errors, token, returnUrl))),
                emailForm => {
                  val emailIsValid =
                    if (emailForm.isEmailVerified) Future.successful(true)
                    else emailConnector.validateEmailAddress(emailForm.mainEmail)
                  emailIsValid flatMap {
                    case true => {
                      preferencesConnector.getPreferencesUnsecured(utr).flatMap {
                        case Some(saPreference) =>
                          Future.successful(Redirect(routes.SaPrefsController.noAction(returnUrl, saPreference.digital)))
                        case None => {
                          preferencesConnector.savePreferencesUnsecured(utr, true, Some(emailForm.mainEmail)).map ( _ =>
                            Redirect(routes.SaPrefsController.confirm(token, returnUrl))
                          )
                        }
                      }
                    }
                    case false => Future.successful(Ok(views.html.sa.prefs.sa_printing_preference_warning_email(emailForm.mainEmail, token, returnUrl)))
                  }
                }
              )
          }
      }
    }


  def submitKeepPaperForm(token: String, encodedReturnUrl: String) =
    ValidateAndDecode(encodedReturnUrl) { returnUrl =>
      WithValidToken(token, returnUrl) {
        utr =>
          Action.async {
            implicit request =>
              preferencesConnector.getPreferencesUnsecured(utr) map {
                case Some(saPreference) =>
                  Redirect(routes.SaPrefsController.noAction(returnUrl, saPreference.digital))
                case None =>
                  preferencesConnector.savePreferencesUnsecured(utr, false)
                  Redirect(returnUrl)
              }
          }
      }
    }
}

case class EmailPreferenceData(email: (String, Option[String]), emailVerified: Option[String]) {
  lazy val isEmailVerified = emailVerified == Some("true")

  def mainEmail = email._1
}