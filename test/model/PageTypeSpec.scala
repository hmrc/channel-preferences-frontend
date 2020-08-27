/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package model
import model.PageType.{ AndroidOptInPage, AndroidOptOutPage, AndroidReOptInPage, AndroidReOptOutPage, IPage, IosOptInPage, IosOptOutPage, IosReOptInPage, IosReOptOutPage, ReOptInPage, TCPage }
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

    """be successfully deserialized from string "AndroidOptInPage" """ in {
      JsString("AndroidOptInPage").as[PageType] must be(AndroidOptInPage)
    }

    """be successfully deserialized from string "AndroidReOptInPage" """ in {
      JsString("AndroidReOptInPage").as[PageType] must be(AndroidReOptInPage)
    }

    """be successfully deserialized from string "AndroidOptOutPage" """ in {
      JsString("AndroidOptOutPage").as[PageType] must be(AndroidOptOutPage)
    }

    """be successfully deserialized from string "AndroidReOptOutPage" """ in {
      JsString("AndroidReOptOutPage").as[PageType] must be(AndroidReOptOutPage)
    }

    """be successfully deserialized from string "IosOptInPage" """ in {
      JsString("IosOptInPage").as[PageType] must be(IosOptInPage)
    }

    """be successfully deserialized from string "IosReOptInPage" """ in {
      JsString("IosReOptInPage").as[PageType] must be(IosReOptInPage)
    }

    """be successfully deserialized from string "IosOptOutPage" """ in {
      JsString("IosOptOutPage").as[PageType] must be(IosOptOutPage)
    }

    """be successfully deserialized from string "IosReOptOutPage" """ in {
      JsString("IosReOptOutPage").as[PageType] must be(IosReOptOutPage)
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

    """be serialized to JsString("AndroidOptInPage")""" in {
      Json.toJson(AndroidOptInPage) must be(JsString("AndroidOptInPage"))
    }

    """be serialized to JsString("AndroidReOptInPage")""" in {
      Json.toJson(AndroidReOptInPage) must be(JsString("AndroidReOptInPage"))
    }

    """be serialized to JsString("AndroidOptOutPage")""" in {
      Json.toJson(AndroidOptOutPage) must be(JsString("AndroidOptOutPage"))
    }

    """be serialized to JsString("AndroidReOptOutPage")""" in {
      Json.toJson(AndroidReOptOutPage) must be(JsString("AndroidReOptOutPage"))
    }

    """be serialized to JsString("IosOptInPage")""" in {
      Json.toJson(IosOptInPage) must be(JsString("IosOptInPage"))
    }

    """be serialized to JsString("IosReOptInPage")""" in {
      Json.toJson(IosReOptInPage) must be(JsString("IosReOptInPage"))
    }

    """be serialized to JsString("IosOptOutPage")""" in {
      Json.toJson(IosOptOutPage) must be(JsString("IosOptOutPage"))
    }

    """be serialized to JsString("IosReOptOutPage")""" in {
      Json.toJson(IosReOptOutPage) must be(JsString("IosReOptOutPage"))
    }
  }

}
