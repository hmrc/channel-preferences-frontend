/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package views.sa.prefs

import _root_.helpers.{ ConfigHelper, LanguageHelper }
import controllers.internal._
import org.jsoup.Jsoup
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.mvc.Call
import views.html.sa.prefs.sa_printing_preference

class SaPrintingPreferenceViewSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[sa_printing_preference]

  "preference print template" should {
    "render the correct content for the IPage cohort " in {
      val document = Jsoup.parse(
        template(
          emailForm = EmailForm(),
          submitPrefsFormAction = Call("GET", "/"),
          cohort = CohortCurrent.ipage
        )(engRequest, messagesInEnglish()).toString())

      document.getElementById("opt-in").hasAttr("checked") mustBe false
    }
  }
}
