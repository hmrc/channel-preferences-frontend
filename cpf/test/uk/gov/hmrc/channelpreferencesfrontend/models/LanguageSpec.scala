/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.channelpreferencesfrontend.models

import org.scalatestplus.play.PlaySpec
import org.scalatest.{ EitherValues, MustMatchers }
import play.api.mvc.PathBindable

class LanguageSpec extends PlaySpec with MustMatchers with EitherValues {

  "Language" should {
    val pathBindable = implicitly[PathBindable[Language]]

    "bind Cymraeg from a URL" in {
      val result = pathBindable.bind("language", Language.Cymraeg.toString)
      result.right.value mustEqual Language.Cymraeg
    }

    "bind English from a URL" in {
      val result = pathBindable.bind("language", Language.English.toString)
      result.right.value mustEqual Language.English
    }

    "unbind Cymraeg" in {
      val result = pathBindable.unbind("language", Language.Cymraeg)
      result mustEqual Language.Cymraeg.toString
    }

    "unbind English" in {
      val result = pathBindable.unbind("language", Language.English)
      result mustEqual Language.English.toString
    }
  }
}
