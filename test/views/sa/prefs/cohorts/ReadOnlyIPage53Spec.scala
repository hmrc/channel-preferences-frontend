/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package views.sa.prefs.cohorts

import controllers.internal
import controllers.internal.EmailForm
import helpers.{ ConfigHelper, LanguageHelper, TestFixtures }
import model.HostContext
import org.jsoup.Jsoup
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import views.html.sa.prefs.cohorts.i_page53

class ReadOnlyIPage53Spec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  implicit lazy val hostContext: HostContext = new HostContext(returnUrl = "", returnLinkText = "")

  val template = app.injector.instanceOf[i_page53]
  "I Page Template" should {
    "render the correct content in english" in {
      val form = EmailForm()
      val document = Jsoup.parse(
        template(form, internal.routes.ChoosePaperlessController.submitForm(TestFixtures.sampleHostContext))(
          engRequest,
          messagesInEnglish(),
          hostContext
        ).toString()
      )

      document
        .getElementsByTag("title")
        .text() mustBe "Choose how to get your tax letters"
      document
        .getElementsByTag("h1")
        .first()
        .text() mustBe "Choose how to get your tax letters"
      document
        .getElementsByTag("p")
        .first()
        .text() mustBe "You can now get your tax letters and information online instead of by post."
      document
        .getElementsByTag("p")
        .get(1)
        .text() mustBe "This means your essential tax letters are:"

      document
        .getElementsByTag("li")
        .get(2)
        .text() mustBe "delivered fast"

      document
        .getElementsByTag("li")
        .get(3)
        .text() mustBe "saved securely"

      document
        .getElementsByTag("li")
        .get(4)
        .text() mustBe "easy to find"
      document
        .getElementsByTag("li")
        .get(5)
        .text() mustBe "simple to share"
      document
        .getElementsByTag("li")
        .get(6)
        .text() mustBe "ready to print, if you need proof"

      document
        .getElementsByTag("h1")
        .get(1)
        .text() mustBe "How do you want to get your tax letters?"
      document
        .getElementById("sps-opt-in-item-hint")
        .text() mustBe "Whenever you have a new online letter, we will let you know by email."
      document.getElementsByClass("govuk-radios__label").first().text() mustBe "Online"
      document
        .getElementById("online-para-1")
        .text() mustBe "Because we cannot send all letters online yet, you will still get some by post."
      document
        .getElementById("online-para-2")
        .text() mustBe "Your responsibilities"
      document
        .getElementById("online-para-3")
        .text() mustBe "1. Sign in to read Because email cannot include personal information, you need to sign in to HMRC online for the details."
      document
        .getElementById("online-para-4")
        .text() mustBe "2. Take action Some online tax letters need you to act, such as reminders to file a return, or to pay penalties. Others are for information, such as changes to your personal tax code."
      document.getElementsByClass("govuk-radios__label").get(1).text() mustBe "By post only"
      document.getElementsByAttributeValue("name", "submitButton").text() mustBe "Continue"

    }

    "render the correct content in welsh" in {
      val form = EmailForm()
      val document = Jsoup.parse(
        template(form, internal.routes.ChoosePaperlessController.submitForm(TestFixtures.sampleHostContext))(
          welshRequest,
          messagesInWelsh(),
          hostContext
        ).toString()
      )
      document
        .getElementsByTag("title")
        .text() mustBe "Dewis sut i gael eich llythyrau treth"
      document
        .getElementsByTag("h1")
        .first()
        .text() mustBe "Dewis sut i gael eich llythyrau treth"
      document
        .getElementsByTag("p")
        .first()
        .text() mustBe "Erbyn hyn gallwch gael eich llythyrau treth a gwybodaeth ar-lein yn lle drwy’r post."
      document
        .getElementsByTag("p")
        .get(1)
        .text() mustBe "Mae hyn yn golygu bod eich llythyrau treth hanfodol:"

      document
        .getElementsByTag("li")
        .get(2)
        .text() mustBe "yn cael eu dosbarthu’n gyflym"

      document
        .getElementsByTag("li")
        .get(3)
        .text() mustBe "yn cael eu cadw’n ddiogel"

      document
        .getElementsByTag("li")
        .get(4)
        .text() mustBe "yn hawdd i’w canfod"
      document
        .getElementsByTag("li")
        .get(5)
        .text() mustBe "yn syml i’w rhannu"
      document
        .getElementsByTag("li")
        .get(6)
        .text() mustBe "yn barod i’w hargraffu, os bydd angen tystiolaeth arnoch"

      document
        .getElementsByTag("h1")
        .get(1)
        .text() mustBe "Sut hoffech gael eich llythyrau treth?"
      document
        .getElementById("sps-opt-in-item-hint")
        .text() mustBe "Pryd bynnag y bydd gennych lythyr ar-lein newydd, byddwn yn anfon e-bost i roi gwybod i chi."
      document.getElementsByClass("govuk-radios__label").first().text() mustBe "Ar-lein"
      document
        .getElementById("online-para-1")
        .text() mustBe "Oherwydd na allwn anfon pob llythyr ar-lein eto, byddwch yn dal i gael rhai llythyrau drwy’r post."
      document
        .getElementById("online-para-2")
        .text() mustBe "Eich cyfrifoldebau"
      document
        .getElementById("online-para-3")
        .text() mustBe "1. Mewngofnodi i’w darllen Oherwydd na all e-bost gynnwys gwybodaeth bersonol, bydd angen i chi fewngofnodi i CThEM ar-lein i gael y manylion."
      document
        .getElementById("online-para-4")
        .text() mustBe "2. Cymryd camau Mae rhai llythyrau treth ar-lein yn gofyn i chi weithredu, megis llythyrau yn eich atgoffa i gyflwyno Ffurflen Dreth neu dalu cosbau. Mae eraill er gwybodaeth, megis newidiadau i’ch cod treth personol."
      document.getElementsByClass("govuk-radios__label").get(1).text() mustBe "Drwy’r post yn unig"
      document.getElementsByAttributeValue("name", "submitButton").text() mustBe "Yn eich blaen"

    }
  }
}
