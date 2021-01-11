/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package views.includes

import controllers.auth.AuthenticatedRequest
import helpers.{ ConfigHelper, LanguageHelper }
import org.jsoup.Jsoup
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import org.scalatestplus.play.PlaySpec
import views.html.includes.yta_header_nav_links

class YtaHeaderNavLinksSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[yta_header_nav_links]

  "Yta Header Nav Links" should {
    "render the correct content in english" in {
      val document = Jsoup.parse(template()(engRequest, messagesInEnglish()).toString())
      document.getElementById("homeNavHref").text() mustBe "Home"
      document.getElementById("accountDetailsNavHref").text() mustBe "Manage account"
      document.getElementsByAttributeValue("href", "/contact/contact-hmrc").text() mustBe "Help and contact"
      document.getElementById("logOutNavHref").text() mustBe "Sign out"
    }

    "render the correct content in welsh" in {
      val document = Jsoup.parse(template()(welshRequest, messagesInWelsh()).toString())
      document.getElementById("homeNavHref").text() mustBe "Hafan"
      document.getElementById("accountDetailsNavHref").text() mustBe "Rheoli'r cyfrif"
      document.getElementsByAttributeValue("href", "/contact/contact-hmrc").text() mustBe "Help a chysylltu"
      document.getElementById("logOutNavHref").text() mustBe "Allgofnodi"
    }
  }
}
