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
import views.html.govuk_wrapper

class GovukWrapperSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[govuk_wrapper]

  "GovukWrapper" should {
    """Display the language switch for "English" page""" in {
      val document = Jsoup.parse(template(title = "", navTitle = "")(engRequest, messagesInEnglish()).toString())
      document.getElementsByTag("li").get(0).text() mustBe "English | Newid yr iaith i'r Gymraeg Cymraeg"
    }

    """Display the language switch for "Welsh" page""" in {
      val document = Jsoup.parse(template(title = "", navTitle = "")(welshRequest, messagesInWelsh()).toString())
      document.getElementsByTag("li").get(0).text() mustBe "Newid yr iaith i'r Saesneg English | Cymraeg"
    }
  }
}
