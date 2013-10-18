package controllers

import play.api.data._
import play.api.mvc.Results._
import play.mvc._
import play.api.data.Forms._
import play.api.mvc.{ AnyContent, Request, Action }
import uk.gov.hmrc.{ TokenExpiredException, PreferencesMicroService, TokenEncryption }
import play.api.{ Logger, Play }
import java.net.URLDecoder
import controllers.service.{ RedirectWhiteListService, FrontEndConfig }

class SaPrefsController extends Controller {

  implicit lazy val preferencesMicroService = new PreferencesMicroService()

  private[controllers] val redirectWhiteListService = new RedirectWhiteListService(FrontEndConfig.redirectDomainWhiteList)

  object WithValidReturnUrl {
    def apply(return_url: String)(action: (Action[AnyContent])) =
      Action {
        request: Request[AnyContent] =>
          redirectWhiteListService.check(return_url) match {
            case true => action(request)
            case false => InternalServerError
          }
      }
  }

  object WithValidToken {
    def apply(token: String, return_url: String)(action: String => (Action[AnyContent])) =
      Action {
        request: Request[AnyContent] =>
          try {
            implicit val utr = SsoPayloadEncryptor.decryptToken(token, FrontEndConfig.tokenTimeout)
            action(utr)(request)
          } catch {
            case e: TokenExpiredException => {
              Logger.error("Unable to validate token", e)
              Redirect(return_url)
            }
          }
      }
  }

  def index(token: String, return_url: String) = WithValidReturnUrl(return_url)(WithValidToken(token, return_url)(
    utr =>
      Action {
        implicit request =>
          preferencesMicroService.getPreferences(utr) match {
            case Some(saPreference) => Redirect(return_url)
            case _ => Ok(views.html.sa_printing_preference(emailForm, token, return_url))
          }
      })
  )

  def confirm(return_url: String) = Action {
    Ok(views.html.sa_printing_preference_confirm(return_url))
  }

  def noAction(return_url: String, digital: Boolean) = Action {
    Ok(views.html.sa_printing_preference_no_action(return_url, digital))
  }

  val emailForm: Form[String] = Form[String](single("email" -> email))

  def submitPrefsForm(token: String, return_url: String) = WithValidReturnUrl(return_url)(WithValidToken(token, return_url)(
    utr =>
      Action {
        request =>
          emailForm.bindFromRequest()(request).fold(
            errors => BadRequest(views.html.sa_printing_preference(errors, token, return_url)),
            email => {

              preferencesMicroService.getPreferences(utr) match {
                case Some(saPreference) => Redirect(routes.SaPrefsController.noAction(return_url, saPreference.digital))
                case None => {
                  preferencesMicroService.savePreferences(utr, true, Some(email))
                  //Play redirect is encoding query params, we need to decode the url to avoid double encoding
                  Redirect(routes.SaPrefsController.confirm(URLDecoder.decode(return_url, "UTF-8")))
                }
              }

            }
          )
      })
  )

  def submitKeepPaperForm(token: String, return_url: String) = WithValidReturnUrl(return_url)(WithValidToken(token, return_url)(
    utr =>
      Action {
        request =>
          preferencesMicroService.getPreferences(utr) match {
            case Some(saPreference) => Redirect(routes.SaPrefsController.noAction(return_url, saPreference.digital))
            case None => {
              preferencesMicroService.savePreferences(utr, false)
              Redirect(return_url)
            }
          }
      })
  )

}

object SsoPayloadEncryptor extends TokenEncryption {
  val encryptionKey = Play.current.configuration.getString("sso.encryption.key").get
}

