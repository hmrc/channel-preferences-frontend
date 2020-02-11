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
