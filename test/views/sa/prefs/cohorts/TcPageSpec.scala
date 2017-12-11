package views.sa.prefs.cohorts

import controllers.internal
import controllers.internal.EmailForm
import helpers.{ConfigHelper, TestFixtures}
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec
import views.html.sa.prefs.cohorts.tc_page

class TcPageSpec  extends UnitSpec with OneAppPerSuite {

  override implicit lazy val app: Application = ConfigHelper.fakeApp

  "Tax Credit Template" should {
    "render the correct content in english" in {
      val email = "a@a.com"
      val form = EmailForm().bind(Map("email.main" -> email))
      val document = Jsoup.parse(tc_page(form, internal.routes.ChoosePaperlessController.submitForm(TestFixtures.sampleHostContext))(FakeRequest("GET", "/"), applicationMessages).toString())
      document.getElementsByClass("header__menu__proposition-name").get(0).text() shouldBe "Tax credits service"
      document.getElementById("email-display").text() shouldBe s"The email address we we will store securely is $email"
      document.getElementsByTag("p").get(1).text() shouldBe "By letting us store your email address, you confirm that you:"
      document.getElementsByTag("li").get(1).text() shouldBe "want to get notifications and prompts about your tax credits"
      document.getElementsByTag("li").get(2).text() shouldBe "will keep your email address up to date using your HMRC online account to make sure you get your email notifications"
      document.getElementsByClass("selectable").get(0).text() shouldBe "Yes, store my email address"
      document.getElementsByClass("selectable").get(1).text() shouldBe "No, I do not want my email address stored"
    }
  }
}