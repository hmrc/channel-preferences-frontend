package controllers.sa.prefs

import play.api.data._
import play.api.data.Forms._
import play.api.mvc.Action
import concurrent.Future
import controllers.common.service.FrontEndConfig
import controllers.sa.prefs.service.SsoPayloadCrypto
import controllers.common.BaseController
import scala.Some
import uk.gov.hmrc.common.microservice.email.EmailConnector
import uk.gov.hmrc.common.microservice.preferences.{SaEmailPreference, SaPreference, PreferencesConnector}
import com.netaporter.uri.dsl._
import uk.gov.hmrc.domain.Email
import controllers.common.domain.EmailPreferenceData

class SaPrefsController(whiteList: Set[String]) extends BaseController {

  implicit lazy val preferencesConnector = new PreferencesConnector()
  implicit lazy val emailConnector = new EmailConnector()
  implicit val wl = whiteList

  def this() = this(FrontEndConfig.redirectDomainWhiteList)

  def index(encryptedToken: String, encodedReturnUrl: String, emailAddressToPrefill: Option[Email]) =
    DecodeAndWhitelist(encodedReturnUrl) { returnUrl =>
      DecryptAndValidate(encryptedToken, returnUrl) { token =>
        Action.async { implicit request =>
          preferencesConnector.getPreferencesUnsecured(token.utr) map {
            case Some(SaPreference(true, Some(SaEmailPreference(emailAddress, _, _)))) =>
              Redirect(returnUrl ? ("emailAddress" -> SsoPayloadCrypto.encrypt(emailAddress)))
            case _ =>
              Ok(
                views.html.sa.prefs.sa_printing_preference(
                  emailForm.fill(EmailPreferenceData(emailAddressToPrefill)),
                  token,
                  returnUrl))
          }
        }
      }
    }

  def confirm(encryptedToken: String, encodedReturnUrl: String) =
    DecodeAndWhitelist(encodedReturnUrl) { returnUrl =>
      DecryptAndValidate(encryptedToken, returnUrl) { token =>
        Action.async { implicit request =>
          preferencesConnector.getPreferencesUnsecured(token.utr).map {
            case Some(SaPreference(true, Some(SaEmailPreference(emailAddress, _, _)))) =>
              Ok(views.html.sa.prefs.sa_printing_preference_confirm(returnUrl ? ("emailAddress" -> SsoPayloadCrypto.encrypt(emailAddress))))
            case _ => PreconditionFailed
          }
        }
      }
    }

  def noAction(encodedReturnUrl: String, digital: Boolean) =
    DecodeAndWhitelist(encodedReturnUrl) { returnUrl =>
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

  def submitPrefsForm(encryptedToken: String, encodedReturnUrl: String) =
    DecodeAndWhitelist(encodedReturnUrl) { returnUrl =>
      DecryptAndValidate(encryptedToken, returnUrl) { token =>
        Action.async { implicit request =>
          emailForm.bindFromRequest()(request).fold(
            errors => Future.successful(BadRequest(views.html.sa.prefs.sa_printing_preference(errors, token, returnUrl))),
            emailForm => {
              val emailIsValid =
                if (emailForm.isEmailVerified) Future.successful(true)
                else emailConnector.validateEmailAddress(emailForm.mainEmail)
              emailIsValid flatMap {
                case true => {
                  preferencesConnector.getPreferencesUnsecured(token.utr).flatMap {
                    case Some(saPreference) =>
                      Future.successful(Redirect(routes.SaPrefsController.noAction(returnUrl, saPreference.digital)))
                    case None => {
                      preferencesConnector.savePreferencesUnsecured(token.utr, true, Some(emailForm.mainEmail)).map ( _ =>
                        Redirect(routes.SaPrefsController.confirm(token.encryptedToken, returnUrl))
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


  def submitKeepPaperForm(encryptedToken: String, encodedReturnUrl: String) =
    DecodeAndWhitelist(encodedReturnUrl) { returnUrl =>
      DecryptAndValidate(encryptedToken, returnUrl) { token =>
        Action.async { implicit request =>
          preferencesConnector.getPreferencesUnsecured(token.utr) map {
            case Some(saPreference) =>
              Redirect(routes.SaPrefsController.noAction(returnUrl, saPreference.digital))
            case None =>
              preferencesConnector.savePreferencesUnsecured(token.utr, false)
              Redirect(returnUrl)
          }
        }
      }
    }
}