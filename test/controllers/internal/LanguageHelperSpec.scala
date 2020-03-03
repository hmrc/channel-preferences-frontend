/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers.internal
import model.Language.{ English, Welsh }
import org.scalatestplus.play.PlaySpec

class LanguageHelperSpec extends PlaySpec with LanguageHelper {
  "lang type" should {
    "create a Welsh object from a cy string" in {
      languageType("cy") must be(Welsh)
    }
    "create a English object from a en string" in {
      languageType("en") must be(English)
    }
  }
}
