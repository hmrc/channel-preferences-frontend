package model

import helpers.ConfigHelper
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application

class HostContextSpec extends WordSpec with Matchers with OneAppPerSuite {

  override implicit lazy val app : Application = ConfigHelper.fakeApp

  "Binding a host context" should {
    val validReturnUrl = "returnUrl" -> Seq("9tNUeRTIYBD0RO+T5WRO7A]==")
    val validReturnLinkText = "returnLinkText" -> Seq("w/PwaxV+KgqutfsU0cyrJQ==")
    val validGenericTermsAndConditions = "termsAndConditions" -> Seq("HYymhRDn1B7qdcKcjIf/1A==")
    val validTaxCreditsTermsAndConditions = "termsAndConditions" -> Seq("J1Vy/h2rVt/JkA1b/lTfgg==")  // taxCredits
    val validEmailAddress = "email" -> Seq("J5lnze8P0QQ8NwFTHVHhVw==")    // test@test.com

    "read the returnURL and returnLinkText if both present" in {
      model.HostContext.hostContextBinder.bind("anyValName", Map(validReturnUrl, validReturnLinkText)) should contain (
        Right(HostContext(returnUrl = "foo", returnLinkText = "bar"))
      )
    }

    "read the returnURL and returnLinkText if both present and termsAndConditions if present" in {
      model.HostContext.hostContextBinder.bind("anyValName", Map(validReturnUrl, validReturnLinkText, validGenericTermsAndConditions)) should contain (
        Right(HostContext(returnUrl = "foo", returnLinkText = "bar", termsAndConditions = Some("generic")))
      )
    }

    "read the returnURL and returnLinkText if both present for taxCredits with emailAddress" in {
      model.HostContext.hostContextBinder.bind("anyValName", Map(validReturnUrl, validReturnLinkText, validTaxCreditsTermsAndConditions, validEmailAddress)) should contain (
        Right(HostContext(returnUrl = "foo", returnLinkText = "bar", termsAndConditions = Some("taxCredits"), Some("test@test.com")))
      )
    }

    "fail when taxCredits does not provide and email" in {
      model.HostContext.hostContextBinder.bind("anyValName", Map(validReturnUrl, validReturnLinkText, validTaxCreditsTermsAndConditions)) should contain (
        Left("TaxCredits must provide email")
      )
    }

    "fail if the returnURL is not present" in {
      model.HostContext.hostContextBinder.bind("anyValName", Map(validReturnLinkText)) should be (Some(Left("No returnUrl query parameter")))
    }

    "fail if the returnLinkText is not present" in {
      model.HostContext.hostContextBinder.bind("anyValName", Map(validReturnUrl)) should be (Some(Left("No returnLinkText query parameter")))
    }
  }

  "Unbinding a host context" should {
    "write out all parameters when headers = Blank" in {
      model.HostContext.hostContextBinder.unbind("anyValName", HostContext(returnUrl = "foo&value", returnLinkText = "bar")) should be (
        "returnUrl=Wa6yuBSzGvUaibkXblJ8aQ%3D%3D&returnLinkText=w%2FPwaxV%2BKgqutfsU0cyrJQ%3D%3D"
      )
    }
  }
}
