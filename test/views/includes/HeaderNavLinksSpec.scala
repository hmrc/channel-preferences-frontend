package views.includes

import helpers.ConfigHelper
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec
import views.html.includes.header_nav_links

class HeaderNavLinksSpec extends UnitSpec with OneAppPerSuite {

  override implicit lazy val app : Application = ConfigHelper.fakeApp

  "Header Nav Links" should {
    "Display the sign out text from messages" in {
      val document = Jsoup.parse(header_nav_links()(FakeRequest("GET", "/"), applicationMessages).toString())
      document.getElementById("logOutNavHref").text() shouldBe "Sign out"
    }
  }
}
