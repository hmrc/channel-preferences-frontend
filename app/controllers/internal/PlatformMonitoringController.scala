package controllers.internal

import java.io.File
import javax.inject.Inject

import play.api.Environment
import play.api.mvc.{Action, Result}
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

// DC-679: Moving monitoring to new controller because controller.Assets.at() does not load resources from /public folder anymore.
class PlatformHealthCheckController  @Inject()(env: Environment) extends FrontendController with AppName {

    def getAsset(fileName: String) = Action.async { implicit request =>
      Future.successful(
        Option(env.classLoader.getResource(s"public/$fileName")).fold[Result](NotFound)(f => Ok.sendFile(new File(f.toURI)))
      )
    }
 }
