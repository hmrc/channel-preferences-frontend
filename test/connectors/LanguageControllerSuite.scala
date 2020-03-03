/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package connectors

import controllers.LanguageController
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._

import scala.concurrent.Future

class LanguageControllerSuite extends PlaySpec with GuiceOneAppPerSuite with Results {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .build()
  val controller = app.injector.instanceOf[LanguageController]
  "Validating english/welsh switch" should {
    "return display english when 'english' selected" in {
      val result: Future[Result] =
        controller.switchToEnglish().apply(FakeRequest().withHeaders("Referer" -> "/paperless/choose"))
      val bodyText: String = contentAsString(result)
      val cook: Cookies = cookies(result)
      cook.get("PLAY_LANG").get.value mustBe "en"
    }
    "return display welsh when 'welsh' selected" in {
      val result: Future[Result] =
        controller.switchToWelsh().apply(FakeRequest().withHeaders("Referer" -> "/paperless/choose"))
      val bodyText: String = contentAsString(result)
      val cook: Cookies = cookies(result)
      cook.get("PLAY_LANG").get.value mustBe "cy"
    }
  }
}
