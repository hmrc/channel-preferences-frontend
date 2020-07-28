/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package model

import controllers.internal.ReOptInPage10
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
    val validWelshLanguage = "language"                          -> Seq("5W0FAIi6JRZBSf4/hwE00w==") // cy
    val validCohortType = "cohort"                               -> Seq("u/n1h8qcsJrhpRofXkhmXg==") // ReOptInPage10

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

    "read the language if present" in {
      model.HostContext.hostContextBinder
        .bind("anyValName", Map(validReturnUrl, validReturnLinkText, validWelshLanguage)) should contain(
        Right(HostContext(returnUrl = "foo", returnLinkText = "bar"))
      )
    }
    "read the cohort if present" in {
      model.HostContext.hostContextBinder
        .bind("anyValName", Map(validReturnUrl, validReturnLinkText, validCohortType)) should contain(
        Right(HostContext(returnUrl = "foo", returnLinkText = "bar", cohort = Some(ReOptInPage10)))
      )
    }
  }

  "Unbinding a host context" should {
    "write out all parameters when headers = Blank" in {
      model.HostContext.hostContextBinder
        .unbind("anyValName", HostContext(returnUrl = "foo&value", returnLinkText = "bar")) should be(
        "returnUrl=Wa6yuBSzGvUaibkXblJ8aQ%3D%3D&returnLinkText=w%2FPwaxV%2BKgqutfsU0cyrJQ%3D%3D"
      )
    }
    "write out all parameters when pageType if ReOptInPage" in {
      model.HostContext.hostContextBinder
        .unbind(
          "anyValName",
          HostContext(
            returnUrl = "foo&value",
            returnLinkText = "bar",
            cohort = Some(ReOptInPage10),
            email = Some("foo@bar.com"))) should be(
        "returnUrl=Wa6yuBSzGvUaibkXblJ8aQ%3D%3D&returnLinkText=w%2FPwaxV%2BKgqutfsU0cyrJQ%3D%3D&email=yCVwXTaKNqm1whFZ7gcFkQ%3D%3D&cohort=u%2Fn1h8qcsJrhpRofXkhmXg%3D%3D"
      )
    }

  }
}
