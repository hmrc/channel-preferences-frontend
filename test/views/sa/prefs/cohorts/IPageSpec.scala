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
import views.html.sa.prefs.cohorts.i_page

class IPageSpec extends UnitSpec with OneAppPerSuite {

  override implicit lazy val app: Application = ConfigHelper.fakeApp

  "Header Nav Links" should {
    "Display the sign out text from messages" in {
      val form = EmailForm().bind(Map("emailAlreadyStored" -> "true"))
      val document = Jsoup.parse(i_page(form, internal.routes.ChoosePaperlessController.submitForm(TestFixtures.sampleHostContext))(FakeRequest("GET", "/"), applicationMessages).toString())
      document.getElementsByClass("lede").first().text() shouldBe "You can choose to get electronic communications instead of letters from HMRC."
      document.getElementsByTag("p").get(2).text() shouldBe "These electronic communications include statutory notices, decisions, estimates and reminders relating to your tax affairs, such as notices to file a tax return, make a payment, penalties due, or information about other matters."
      document.getElementsByTag("p").get(3).text() shouldBe "When you have a new electronic communication we will send you an email notification requiring you to log in to your HMRC online account."
      document.getElementsByTag("h2").get(0).text() shouldBe "Go paperless now"
      document.getElementsByClass("selectable").get(0).text() shouldBe "Yes, send me electronic communications"
      document.getElementsByTag("p").get(4).childNodes().get(0).toString.trim shouldBe "You have signed up for digital communications for Tax Credits with this email address."
      document.getElementsByTag("p").get(4).childNodes().get(2).toString.trim shouldBe "If you wish to change the email address you can do this later within manage your account"
      document.getElementsByTag("p").get(5).text() shouldBe "By signing up, you confirm that you:"
      document.getElementsByTag("li").get(1).text() shouldBe "want to receive statutory notices, decisions, estimates and reminders electronically in connection with your tax affairs"
      document.getElementsByTag("li").get(2).text() shouldBe "will keep your communications preferences and email address up to date using your HMRC online account to make sure you get your email notifications"
      document.getElementsByClass("selectable").get(1).text() shouldBe "No, I want to keep receiving letters"
    }
  }
}