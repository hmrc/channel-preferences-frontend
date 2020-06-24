/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package model
import model.PageType.{ IPage, TCPage }
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{ JsString, Json }

class PageTypeSpec extends PlaySpec {

  "PageType" must {
    """be successfully deserialized from string "IPage" """ in {
      JsString("IPage").as[PageType] must be(IPage)
    }
    """be successfully deserialized from string "TCPage" """ in {
      JsString("TCPage").as[PageType] must be(TCPage)
    }

    """throw NoSuchElementException when deserialized from invalid string """.stripMargin in {
      intercept[NoSuchElementException] {
        JsString("foobar").as[PageType]
      }
    }

    """be serialized to JsString("IPage")""" in {
      Json.toJson(IPage) must be(JsString("IPage"))
    }

    """be serialized to JsString("cy")""" in {
      Json.toJson(TCPage) must be(JsString("TCPage"))
    }

  }

}
