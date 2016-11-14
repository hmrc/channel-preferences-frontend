package controllers.internal

import play.api.{Application, Play}
import play.api.mvc.Action
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

// DC-679: Moving monitoring to new controller because controller.Assets.at() does not load resources from /public folder anymore.
class PlatformHealthCheckController extends FrontendController with AppName {

    def getAsset(fileName: String) = Action.async { implicit request =>
      val file = Play.current.getFile(s"public/$fileName")
      if (file.exists()) {
        Future.successful(Ok.sendFile(file))
      } else {
        Future.successful(NotFound)
      }
    }
}
