/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package views.sa.prefs.cohorts

import controllers.internal
import controllers.internal.EmailForm
import helpers.{ ConfigHelper, LanguageHelper, TestFixtures }
import org.jsoup.Jsoup
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import views.html.sa.prefs.cohorts.reoptin_page10

class ReadOnlyReOptInPage10Spec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp

  val template = app.injector.instanceOf[reoptin_page10]
  "ReOptIn10 Template" should {
    "render the correct content in english" in {
      val form = EmailForm()
      val document = Jsoup.parse(
        template(form, internal.routes.ChoosePaperlessController.submitForm(TestFixtures.sampleHostContext))(
          engRequest,
          messagesInEnglish()).toString())

      document
        .getElementsByTag("title")
        .text() mustBe "Don't lose access to your online tax documents"
      document
        .getElementsByTag("h1")
        .text() mustBe "Don't lose access to your online tax documents"
      document
        .getElementsByClass("lede")
        .first()
        .text() mustBe "You can choose to continue to get some of your tax documents and information sent through your HMRC online account instead of by post."
      document
        .getElementsByTag("p")
        .get(2)
        .text() mustBe "Choose how to get your legal notices, penalty notices and tax letters"

      document
        .getElementsByTag("li")
        .get(2)
        .text() mustBe "Legal notices to file tax return"

      document
        .getElementsByTag("li")
        .get(3)
        .text() mustBe "Late filing penalty notices"

      document
        .getElementsByTag("li")
        .get(4)
        .text() mustBe "Late payment penalty notices"

      document
        .getElementsByTag("p")
        .get(3)
        .text() mustBe "We may also send you other messages, including information about your personal tax code, if you have one"

      document
        .getElementsByTag("h2")
        .get(0)
        .text() mustBe "How do you want to get your legal notices, penalty notices and tax letters?"

      document
        .getElementsByTag("p")
        .get(4)
        .childNodes()
        .get(0)
        .toString
        .trim mustBe "We’ll email to tell you when you have a new item in your online account."

      document.getElementsByClass("selectable").get(0).text() mustBe "Through my HMRC online account"

      document
        .getElementsByTag("p")
        .get(5)
        .childNodes()
        .get(0)
        .toString
        .trim mustBe "Because we cannot send all letters online yet, you will continue to get some by post."

      document
        .getElementById("terms-and-conditions")
        .attr("href") mustBe "https://www.tax.service.gov.uk/information/terms?lang=eng#secure"
      document.getElementsByClass("selectable").get(1).text() mustBe "By post only"
      document.getElementById("privacy-policy").text() must include("read the privacy notice")
      document.getElementsByAttributeValue("name", "submitButton").text() mustBe "Continue"

    }

    "render the correct content in welsh" in {
      val form = EmailForm()
      val document = Jsoup.parse(
        template(form, internal.routes.ChoosePaperlessController.submitForm(TestFixtures.sampleHostContext))(
          welshRequest,
          messagesInWelsh()).toString())

      document
        .getElementsByTag("title")
        .text() mustBe "Peidiwch â cholli mynediad at eich dogfennau treth ar-lein"
      document
        .getElementsByTag("h1")
        .text() mustBe "Peidiwch â cholli mynediad at eich dogfennau treth ar-lein"
      document
        .getElementsByClass("lede")
        .first()
        .text() mustBe "Gallwch ddewis parhau i gael rhywfaint o’ch gwybodaeth a dogfennau treth drwy’ch cyfrif CThEM ar-lein, yn hytrach na thrwy’r post."
      document
        .getElementsByTag("p")
        .get(2)
        .text() mustBe "Dewis sut i gael eich hysbysiadau cyfreithiol, hysbysiadau o gosb a llythyrau treth"

      document
        .getElementsByTag("li")
        .get(2)
        .text() mustBe "Hysbysiadau cyfreithiol i gyflwyno Ffurflen Dreth"

      document
        .getElementsByTag("li")
        .get(3)
        .text() mustBe "Hysbysiadau o gosb am gyflwyno’n hwyr"

      document
        .getElementsByTag("li")
        .get(4)
        .text() mustBe "Hysbysiadau o gosb am dalu’n hwyr"

      document
        .getElementsByTag("p")
        .get(3)
        .text() mustBe "Mae’n bosibl y byddwn hefyd yn anfon negeseuon eraill atoch, gan gynnwys gwybodaeth am eich cod treth personol, os oes un gennych, neu am eich hunanasesiad."

      document
        .getElementsByTag("h2")
        .get(0)
        .text() mustBe "Sut yr hoffech gael eich hysbysiadau cyfreithiol, hysbysiadau o gosb a llythyrau treth?"

      document
        .getElementsByTag("p")
        .get(4)
        .childNodes()
        .get(0)
        .toString
        .trim mustBe "Byddwn yn anfon e-bost atoch i roi gwybod i chi pan fydd eitem newydd yn eich cyfrif ar-lein."

      document.getElementsByClass("selectable").get(0).text() mustBe "Drwy fy nghyfrif CThEM ar-lein"

      document
        .getElementsByTag("p")
        .get(5)
        .childNodes()
        .get(0)
        .toString
        .trim mustBe "Oherwydd na allwn anfon pob llythyr ar-lein eto, byddwch yn parhau i gael rhai llythyrau drwy’r post."

      document
        .getElementById("terms-and-conditions")
        .attr("href") mustBe "https://www.tax.service.gov.uk/information/terms?lang=cym"
      document.getElementsByClass("selectable").get(1).text() mustBe "Drwy’r post yn unig"
      document.getElementById("privacy-policy").text() must include("darllenwch yr hysbysiad preifatrwydd")
      document.getElementsByAttributeValue("name", "submitButton").text() mustBe "Yn eich blaen"
    }
  }
}