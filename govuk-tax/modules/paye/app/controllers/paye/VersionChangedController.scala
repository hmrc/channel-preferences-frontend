package controllers.paye

import play.api.mvc.{Controller, Action}
import views.html.paye.version_changed

object VersionChangedController extends Controller {

  def versionChanged = Action {
    Ok(version_changed())
  }

}
