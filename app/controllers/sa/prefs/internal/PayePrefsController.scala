package controllers.sa.prefs.internal

import play.api.mvc.{Action, AnyContent}
import play.twirl.api.Html
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

object PayePrefsController extends FrontendController {
  def displayPrefsForm(): Action[AnyContent] = Action.async {
    Future.successful(Ok(Html("<h1>Paye page</h1>")))
  }
}
