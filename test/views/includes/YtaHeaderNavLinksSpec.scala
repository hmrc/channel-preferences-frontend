package views.includes

import controllers.auth.AuthenticatedRequest
import helpers.{ConfigHelper, WelshLanguage}
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec
import views.html.includes.yta_header_nav_links

class YtaHeaderNavLinksSpec extends UnitSpec with OneAppPerSuite with WelshLanguage {

  override implicit lazy val app: Application = ConfigHelper.fakeApp

  "Yta Header Nav Links" should {
    "render the correct content in english" in {
      val document = Jsoup.parse(yta_header_nav_links()(AuthenticatedRequest(FakeRequest("GET", "/"), None, None, None, None), applicationMessages).toString())
      document.getElementById("homeNavHref").text() shouldBe "Home"
      document.getElementById("accountDetailsNavHref").text() shouldBe "Manage account"
      document.getElementsByAttributeValue("href", "/contact/contact-hmrc").text() shouldBe "Help and contact"
      document.getElementById("logOutNavHref").text() shouldBe "Sign out"
    }

    "render the correct content in welsh" in {
      val document = Jsoup.parse(yta_header_nav_links()(welshRequest, messagesInWelsh(applicationMessages)).toString())
      document.getElementById("homeNavHref").text() shouldBe "Hafan"
      document.getElementById("accountDetailsNavHref").text() shouldBe "Rheoli'r cyfrif"
      document.getElementsByAttributeValue("href", "/contact/contact-hmrc").text() shouldBe "Help a chysylltu"
      document.getElementById("logOutNavHref").text() shouldBe "Allgofnodi"
    }
  }
}
