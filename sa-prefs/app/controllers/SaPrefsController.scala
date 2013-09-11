package controllers

import play.api.data._
import play.api.mvc.Results._
import play.mvc._
import play.api.data.Forms._
import org.omg.CosNaming.NamingContextPackage.NotFound
import play.api.mvc.Action

case class PrintingPreferenceForm(email: String)

object SaPrefsController extends Controller {

  def index(token: String, redirectUrl: String) = Action {
    Ok(views.html.sa_printing_preference(printPrefsForm, token, redirectUrl))
  }

  def confirm(token: String, redirectUrl: String) = Action {
    Ok(views.html.sa_printing_preference_confirm(token, redirectUrl))
  }

  val printPrefsForm: Form[PrintingPreferenceForm] = Form(
    mapping(
      "email" -> email
    ) {
        prefs => PrintingPreferenceForm(prefs)
      } {
        form => Some(form.email)
      }
  )

  def submitPrefsForm(token: String, redirectUrl: String) = Action { request =>
    printPrefsForm.bindFromRequest()(request).fold(
      errors => BadRequest(views.html.sa_printing_preference(printPrefsForm, token, redirectUrl)),
      printPrefsForm => {
        Redirect(routes.SaPrefsController.confirm(token, redirectUrl))
      }
    )
  }
}