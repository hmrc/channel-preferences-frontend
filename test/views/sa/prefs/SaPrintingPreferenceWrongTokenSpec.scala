package views.sa.prefs

import _root_.helpers.{ConfigHelper, WelshLanguage}
import controllers.auth.AuthenticatedRequest
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec
import views.html.sa.prefs.sa_printing_preference_wrong_token

class SaPrintingPreferenceWrongTokenSpec extends UnitSpec with OneAppPerSuite with WelshLanguage {

  override implicit lazy val app: Application = ConfigHelper.fakeApp

  "sa printing preferences wrong token template" should {
    "render the correct content in english" in {
      val document = Jsoup.parse(sa_printing_preference_wrong_token()(AuthenticatedRequest(FakeRequest("GET", "/"), None, None, None, None), applicationMessages).toString())

      document.getElementsByTag("title").first().text() shouldBe "You've used a link that has now expired"
    }

    "render the correct content in welsh" in {
      val document = Jsoup.parse(sa_printing_preference_wrong_token()(welshRequest, messagesInWelsh(applicationMessages)).toString())

      document.getElementsByTag("title").first().text() shouldBe "Rydych wedi defnyddio cysylltiad sydd bellach wedi dod i ben"
      document.getElementById("failure-heading").text() shouldBe "Rydych wedi defnyddio cysylltiad sydd bellach wedi dod i ben"
      document.getElementById("success-message").text() shouldBe "Mae'n bosibl ei fod wedi cael ei anfon i hen gyfeiriad e-bost neu un gwahanol. Dylech ddefnyddio'r cysylltiad yn yr e-bost dilysu diwethaf a anfonwyd i'r cyfeiriad e-bost a nodwyd gennych."
    }
  }
}
