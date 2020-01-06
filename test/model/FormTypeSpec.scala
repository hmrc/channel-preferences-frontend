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

import helpers.ConfigHelper
import model.FormType.formTypeBinder
import org.scalatest.{ Matchers, WordSpec }
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application

class FormTypeSpec extends WordSpec with Matchers with GuiceOneAppPerSuite with ConfigHelper {

  override implicit lazy val app: Application = fakeApp

  "Bind known form types" should {
    for {
      (formType, formTypeName) <- Seq(SaAll -> SaAll.value, NoticeOfCoding -> NoticeOfCoding.value)
    } s"work for $formTypeName to $formType" in {
      formTypeBinder.bind("", formTypeName) should be(Right(formType))
    }
  }

  "Binding of an unknown form-type should be not successful" in {
    formTypeBinder.bind("", "unrecognized") should be(Left("unrecognized is not a valid form-type"))
  }

  "Unbinding a formType object" should {
    "return formType's value" in {
      formTypeBinder.unbind("", SaAll) should be(SaAll.value)
    }
  }
}
