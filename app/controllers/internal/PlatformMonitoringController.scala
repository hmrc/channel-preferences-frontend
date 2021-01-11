/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers.internal

import controllers.Assets
import javax.inject.Inject
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.ExecutionContext

// DC-679: Moving monitoring to new controller because we require to disable auditing.

class PlatformHealthCheckController @Inject()(mcc: MessagesControllerComponents)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) {
  def getAsset(fileName: String) = Assets.at(path = "/public", file = fileName)
}
