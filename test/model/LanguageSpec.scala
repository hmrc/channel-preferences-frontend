/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
      Json.toJson(English) must be(JsString("en"))
    }

    """Language(cy) must succefully serialized to JsString("cy")""" in {
      Json.toJson(Welsh) must be(JsString("cy"))
    }

  }

}
