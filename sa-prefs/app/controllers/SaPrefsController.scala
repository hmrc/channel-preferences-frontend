package controllers

import play.api.data._
import play.api.mvc.Results._
import play.mvc._
import play.api.data.Forms._
import play.api.mvc.Action
import uk.gov.hmrc.{ SaMicroService, TokenEncryption }
import play.api.Play
import java.net.{ URLEncoder, URLDecoder }

class SaPrefsController extends Controller {

  implicit lazy val saMicroService = new SaMicroService()

  def index(token: String, return_url: String) = Action { implicit request =>
    val utr = SsoPayloadEncryptor.decryptToken(token)
    Ok(views.html.sa_printing_preference(emailForm, token, return_url))
  }

  def confirm(return_url: String) = Action {
    Ok(views.html.sa_printing_preference_confirm(return_url))
  }

  val emailForm: Form[String] = Form[String](single("email" -> email))

  def submitPrefsForm(token: String, return_url: String) = Action { request =>
    emailForm.bindFromRequest()(request).fold(
      errors => BadRequest(views.html.sa_printing_preference(errors, token, return_url)),
      email => {

        saMicroService.savePreferences(SsoPayloadEncryptor.decryptToken(token), true, Some(email))

        //Play redirect is encoding query params, we need to decode the url to avoid double encoding
        Redirect(routes.SaPrefsController.confirm(URLDecoder.decode(return_url, "UTF-8")))
      }
    )
  }

  def submitKeepPaperForm(token: String, return_url: String) = Action { request =>

    saMicroService.savePreferences(SsoPayloadEncryptor.decryptToken(token), false)
    Redirect(return_url)
  }

}

object SsoPayloadEncryptor extends TokenEncryption {
  val encryptionKey = Play.current.configuration.getString("sso.encryption.key").get
}

