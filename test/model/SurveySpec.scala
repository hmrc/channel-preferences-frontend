/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package model

import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{ JsObject, JsString, Json }
import model.SurveyType.StandardInterruptOptOut

class SurveySpec extends PlaySpec {

  "SurveyType" should {
    """deserialize SandardInterruptOptOut from string "StandardInterrupOptOut" """ in {
      JsString("StandardInterruptOptOut").as[SurveyType] must be(SurveyType.StandardInterruptOptOut)
    }

    """serialize StandardInterruptOptOut to JsString("StandardInterruptOptOut")""" in {
      Json.toJson(StandardInterruptOptOut) must be(JsString("StandardInterruptOptOut"))
    }

    "serialize and deserialize Survey" in {
      val date = new DateTime(2015, 5, 13, 0, 0, 0).withZone(UTC)
      val fixture = Json
        .parse(s"""
                  |{
                  |  "surveyType": "StandardInterruptOptOut",
                  |  "completedAt": {"$$date": ${date.getMillis}}
                  |}""".stripMargin)
        .as[JsObject]
      fixture.as[Survey] must be(Survey(StandardInterruptOptOut, date))
      Json.toJson(Survey(StandardInterruptOptOut, date)) must be(fixture)
    }

  }

}
