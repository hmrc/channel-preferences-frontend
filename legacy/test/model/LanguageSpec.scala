/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package model
import model.Language.{ English, Welsh }
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{ JsString, Json }

class LanguageSpec extends PlaySpec {

  "Language" must {
    """Language(cy) must be successfully deserialized from string "cy" """ in {
      JsString("cy").as[Language] must be(Welsh)
    }
    """Language(en) must be successfully deserialized from string "en" """ in {
      JsString("en").as[Language] must be(English)
    }

    """Language(en) must be successfully deserialized from any other string """ in {
      JsString("foobar").as[Language] must be(English)
    }

    """Language(en) must succefully serialized to JsString("en")""" in {
      Json.toJson[Language](Language.English) must be(JsString("en"))
    }

    """Language(cy) must succefully serialized to JsString("cy")""" in {
      Json.toJson[Language](Language.Welsh) must be(JsString("cy"))
    }

  }

}
