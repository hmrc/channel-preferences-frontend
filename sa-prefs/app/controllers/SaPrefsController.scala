package controllers

import play.api.data._
import play.api.mvc.Results._
import play.mvc._
import play.api.data.Forms._
import play.api.mvc.Action
import uk.gov.hmrc.{ MicroServiceConfig, MicroService }
import uk.gov.hmrc.Transform._
import play.api.libs.json.Json

class SaPrefsController extends Controller with TokenEncryption {

  implicit lazy val saMicroService = new SaMicroService()

  def index(token: String, redirectUrl: String) = Action { implicit request =>
    Ok(views.html.sa_printing_preference(emailForm, token, redirectUrl))
  }

  def confirm(token: String, redirectUrl: String) = Action {
    Ok(views.html.sa_printing_preference_confirm(token, redirectUrl))
  }

  val emailForm: Form[String] = Form[String](single("email" -> email))

  def submitPrefsForm(token: String, redirectUrl: String) = Action { request =>
    emailForm.bindFromRequest()(request).fold(
      errors => BadRequest(views.html.sa_printing_preference(errors, token, redirectUrl)),
      email => {

        saMicroService.savePreferences(decryptToken(token), true, Some(email))
        Redirect(routes.SaPrefsController.confirm(token, redirectUrl))
      }
    )
  }
}

trait TokenEncryption {
  def decryptToken(token: String): String = {
    "A-UTR"
  }
}

class SaMicroService extends MicroService {

  override val serviceUrl = MicroServiceConfig.saServiceUrl

  def savePreferences(utr: String, digital: Boolean, email: Option[String] = None) {
    httpPutNoResponse(s"/sa/utr/$utr/preferences", Json.parse(toRequestBody(SaPreference(digital, email))))
  }

  case class SaPreference(digital: Boolean, email: Option[String] = None)
}