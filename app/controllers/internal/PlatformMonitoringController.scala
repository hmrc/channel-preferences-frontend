package controllers.internal

import controllers.Assets
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.frontend.controller.FrontendController

// DC-679: Moving monitoring to new controller because we require to disable auditing.
class PlatformHealthCheckController extends FrontendController with AppName {

    def getAsset(fileName: String) = Assets.at(path="/public", file = fileName)
}
