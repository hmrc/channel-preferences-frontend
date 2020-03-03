/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package views

import _root_.helpers.{ ConfigHelper, LanguageHelper }
import controllers.auth.AuthenticatedRequest
import org.jsoup.Jsoup
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.emailaddress.ObfuscatedEmailAddress
import org.scalatestplus.play.PlaySpec
import views.html.main

class MainSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[main]

  "main template" should {
    "render the correct content in english" in {
      val email = ObfuscatedEmailAddress("a@a.com")
      val document = Jsoup.parse(template("title")(Html("Some HTML"))(engRequest, messagesInEnglish()).toString())

      document.getElementsByClass("header__menu__proposition-name").get(0).text() mustBe "Your tax account"
    }

    "render the correct content in welsh" in {
      val email = ObfuscatedEmailAddress("a@a.com")
      val document = Jsoup.parse(template("title")(Html("Some HTML"))(welshRequest, messagesInWelsh()).toString())

      document.getElementsByClass("header__menu__proposition-name").get(0).text() mustBe "Eich cyfrif treth"
    }
  }
}
