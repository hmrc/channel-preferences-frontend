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
import org.scalatest.{ Matchers, WordSpec }
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application

class HostContextSpec extends WordSpec with Matchers with GuiceOneAppPerSuite with ConfigHelper {

  override implicit lazy val app: Application = fakeApp

  "Binding a host context" should {
    val validReturnUrl = "returnUrl"                             -> Seq("9tNUeRTIYBD0RO+T5WRO7A]==")
    val validReturnLinkText = "returnLinkText"                   -> Seq("w/PwaxV+KgqutfsU0cyrJQ==")
    val validAlreadyOptedInUrl = "alreadyOptedInUrl"             -> Seq("Co1YzTJv/KYa5nRQXLAqlw==")
    val validGenericTermsAndConditions = "termsAndConditions"    -> Seq("HYymhRDn1B7qdcKcjIf/1A==")
    val validTaxCreditsTermsAndConditions = "termsAndConditions" -> Seq("J1Vy/h2rVt/JkA1b/lTfgg==") // taxCredits
    val validEmailAddress = "email"                              -> Seq("J5lnze8P0QQ8NwFTHVHhVw==") // test@test.com

    "read the returnURL and returnLinkText if both present" in {
      model.HostContext.hostContextBinder.bind("anyValName", Map(validReturnUrl, validReturnLinkText)) should contain(
        Right(HostContext(returnUrl = "foo", returnLinkText = "bar"))
      )
    }

    "read the returnURL and returnLinkText if both present and alreadyOptedInUrl if present" in {
      model.HostContext.hostContextBinder
        .bind("anyValName", Map(validReturnUrl, validReturnLinkText, validAlreadyOptedInUrl)) should contain(
        Right(HostContext(returnUrl = "foo", returnLinkText = "bar", alreadyOptedInUrl = Some("AnotherUrl")))
      )
    }

    "read the returnURL and returnLinkText if both present and termsAndConditions if present" in {
      model.HostContext.hostContextBinder
        .bind("anyValName", Map(validReturnUrl, validReturnLinkText, validGenericTermsAndConditions)) should contain(
        Right(HostContext(returnUrl = "foo", returnLinkText = "bar", termsAndConditions = Some("generic")))
      )
    }

    "read the returnURL and returnLinkText if both present for taxCredits with emailAddress" in {
      model.HostContext.hostContextBinder.bind(
        "anyValName",
        Map(validReturnUrl, validReturnLinkText, validTaxCreditsTermsAndConditions, validEmailAddress)) should contain(
        Right(
          HostContext(
            returnUrl = "foo",
            returnLinkText = "bar",
            termsAndConditions = Some("taxCredits"),
            Some("test@test.com")))
      )
    }

    "fail when taxCredits does not provide and email" in {
      model.HostContext.hostContextBinder
        .bind("anyValName", Map(validReturnUrl, validReturnLinkText, validTaxCreditsTermsAndConditions)) should contain(
        Left("TaxCredits must provide email")
      )
    }

    "fail if the returnURL is not present" in {
      model.HostContext.hostContextBinder.bind("anyValName", Map(validReturnLinkText)) should be(
        Some(Left("No returnUrl query parameter")))
    }

    "fail if the returnLinkText is not present" in {
      model.HostContext.hostContextBinder.bind("anyValName", Map(validReturnUrl)) should be(
        Some(Left("No returnLinkText query parameter")))
    }
  }

  "Unbinding a host context" should {
    "write out all parameters when headers = Blank" in {
      model.HostContext.hostContextBinder
        .unbind("anyValName", HostContext(returnUrl = "foo&value", returnLinkText = "bar")) should be(
        "returnUrl=Wa6yuBSzGvUaibkXblJ8aQ%3D%3D&returnLinkText=w%2FPwaxV%2BKgqutfsU0cyrJQ%3D%3D"
      )
    }
  }
}
