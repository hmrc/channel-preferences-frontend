package connectors


import controllers.LanguageController

import scala.concurrent.Future
import org.scalatestplus.play._
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._

class LanguageControllerSuite extends PlaySpec with Results {

  "Validating english/welsh switch" should {
    "return display english when 'english' selected" in {
      running(FakeApplication()) {
        val controller = new LanguageController() {}
        val result: Future[Result] = controller.switchToEnglish().apply(FakeRequest().withHeaders( "Referer" -> "/paperless/choose" ))
        val bodyText: String = contentAsString(result)
        val cook: Cookies = cookies(result)
        cook.get("PLAY_LANG").get.value mustBe "en"
      }
    }
    "return display welsh when 'welsh' selected" in {
      running(FakeApplication()) {
        val controller = new LanguageController() {}
        val result: Future[Result] = controller.switchToWelsh().apply(FakeRequest().withHeaders( "Referer" -> "/paperless/choose" ))
        val bodyText: String = contentAsString(result)
        val cook: Cookies = cookies(result)
        cook.get("PLAY_LANG").get.value mustBe "cy"
      }
    }
  }
}