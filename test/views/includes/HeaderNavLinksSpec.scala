/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package views.includes

import helpers.{ ConfigHelper, LanguageHelper }
import org.jsoup.Jsoup
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import org.scalatestplus.play.PlaySpec
import views.html.includes.header_nav_links

class HeaderNavLinksSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[header_nav_links]

  "Header Nav Links" should {
    "Display the sign out text from messages" in {
      val document = Jsoup.parse(template()(engRequest, messagesInEnglish()).toString())
      document.getElementById("logOutNavHref").text() mustBe "Sign out"
    }

    "Display the sign out text from messages in welsh" in {
      val document = Jsoup.parse(template()(welshRequest, messagesInWelsh()).toString())
      document.getElementById("logOutNavHref").text() mustBe "Allgofnodi"
    }
  }
}
