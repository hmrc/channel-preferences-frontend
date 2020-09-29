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
import controllers.internal.LanguageForm
import views.html._
import play.api.mvc.Call

import play.api.test.FakeRequest

import controllers.auth.AuthenticatedRequest

class ChangeLanguageSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[change_language]

  val authRequest = AuthenticatedRequest(FakeRequest(), None, None, None, None)
  "change language page" should {
    val form = LanguageForm()
    "render the correct content in english" in {
      val document =
        Jsoup.parse(
          template(form, Call("/myurl", "GET"))(authRequest, messagesInEnglish(), TestFixtures.sampleHostContext)
            .toString())

      document
        .getElementsByClass("govuk-fieldset__heading")
        .first()
        .text() mustBe "Get your paperless email notification in Welsh"
    }

    "render the correct content in welsh" in {
      val document =
        Jsoup.parse(
          template(form, Call("/myurl", "GET"))(authRequest, messagesInWelsh(), TestFixtures.sampleHostContext)
            .toString())

      document
        .getElementsByClass("govuk-fieldset__heading")
        .first()
        .text() mustBe "Cael eich hysbysiadau di-bapur drwy e-bost yn Gymraeg"
    }
  }
}
