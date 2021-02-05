/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.channelpreferencesfrontend.controllers.swagger

import com.iheart.playSwagger.PrefixDomainModelQualifier
import javax.inject.{ Inject, Singleton }
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

@Singleton
class ApiSpecsController @Inject() (
  mcc: MessagesControllerComponents
) extends FrontendController(mcc) {

  implicit val cl: ClassLoader = getClass.getClassLoader
  val domainPackage = "uk.gov.hmrc.channelpreferencesfrontend"
  lazy val generator =
    com.iheart.playSwagger.SwaggerSpecGenerator(
      swaggerV3 = true,
      modelQualifier = PrefixDomainModelQualifier(domainPackage),
      apiVersion = Some(uk.gov.hmrc.channelpreferencesfrontend.BuildInfo.version)
    )

  lazy val swagger = Action { _ =>
    generator
      .generate("cpf.routes")
      .fold(e => InternalServerError(s"Couldn't generate swagger. ${e.getMessage()}"), s => Ok(Json.prettyPrint(s)))
  }

  def specs: Action[AnyContent] = swagger
}
