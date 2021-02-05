/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.channelpreferencesfrontend.controllers

import javax.inject.{ Inject, Singleton }

import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.channelpreferencesfrontend.config.AppConfig
import uk.gov.hmrc.channelpreferencesfrontend.views.html.HelloWorldPage

import scala.concurrent.Future

@Singleton
class HelloWorldController @Inject() (
  appConfig: AppConfig,
  mcc: MessagesControllerComponents,
  helloWorldPage: HelloWorldPage
) extends FrontendController(mcc) {

  implicit val config: AppConfig = appConfig

  val helloWorld: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(helloWorldPage()))
  }
}
