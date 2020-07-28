/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package model
import model.PageType.{ IPage, ReOptInPage, TCPage }
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{ JsResultException, JsString, Json }

class PageTypeSpec extends PlaySpec {

  "PageType" must {
    """be successfully deserialized from string "IPage" """ in {
      JsString("IPage").as[PageType] must be(IPage)
    }
    """be successfully deserialized from string "TCPage" """ in {
      JsString("TCPage").as[PageType] must be(TCPage)
    }

    """be successfully deserialized from string "ReOptInPage" """ in {
      JsString("ReOptInPage").as[PageType] must be(ReOptInPage)
    }

    """throw NoSuchElementException when deserialized from invalid string """.stripMargin in {
      intercept[JsResultException] {
        JsString("foobar").as[PageType]
      }
    }

    """be serialized to JsString("IPage")""" in {
      Json.toJson(IPage) must be(JsString("IPage"))
    }

    """be serialized to JsString("TCPage")""" in {
      Json.toJson(TCPage) must be(JsString("TCPage"))
    }

    """be serialized to JsString("ReOptInPage")""" in {
      Json.toJson(ReOptInPage) must be(JsString("ReOptInPage"))
    }

  }

}
