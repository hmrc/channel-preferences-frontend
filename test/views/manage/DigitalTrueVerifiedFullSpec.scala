/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package views.manage

import _root_.helpers.{ ConfigHelper, LanguageHelper, TestFixtures }
import connectors.EmailPreference
import org.jsoup.Jsoup
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import org.scalatestplus.play.PlaySpec
import views.html.manage._

import play.api.test.FakeRequest

import controllers.auth.AuthenticatedRequest

class DigitalTrueVerifiedFullSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[digital_true_verified_full]

  "settings page for digital true verified" should {
    "render the correct content in english" in {
      val emailAddress = "a@a.com"
      val email = EmailPreference(emailAddress, true, true, false, None)

      val authRequest = AuthenticatedRequest(FakeRequest(), None, None, None, None)
      val document =
        Jsoup.parse(template(email)(authRequest, messagesInEnglish(), TestFixtures.sampleHostContext).toString())

      document.getElementById("saCheckSettings").text() mustBe "Check your settings"
    }

    "render the correct content in welsh" in {
      val emailAddress = "a@a.com"
      val email = EmailPreference(emailAddress, true, true, false, None)
      val document =
        Jsoup.parse(template(email)(welshRequest, messagesInWelsh(), TestFixtures.sampleHostContext).toString())

      document.getElementById("saCheckSettings").text() mustBe "Gwirio’ch gosodiadau"
    }
  }
}
