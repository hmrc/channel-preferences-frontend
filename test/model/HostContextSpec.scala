package model

import model.HostContext.Headers.{Blank, YTA}
import org.scalatest.{Matchers, WordSpec}
import uk.gov.hmrc.play.test.WithFakeApplication

class HostContextSpec extends WordSpec with Matchers with WithFakeApplication {

  "Binding a host context" should {
    val validReturnUrl = "returnUrl" -> Seq("9tNUeRTIYBD0RO+T5WRO7A]==")
    val validReturnLinkText = "returnLinkText" -> Seq("w/PwaxV+KgqutfsU0cyrJQ==")
    val validBlankHeaders = "headers" -> Seq("47pBbPf0Mz0gcFMRdx8qUQ==")
    val validYTAHeaders = "headers" -> Seq("+OwkJJim0p+X5aV+SV2Cew==")

    "read the returnURL and returnLinkText and Blank header if all present" in {
      model.HostContext.hostContextBinder.bind("anyValName", Map(validReturnUrl, validReturnLinkText, validBlankHeaders)) should contain (
        Right(HostContext(returnUrl = "foo", returnLinkText = "bar", headers = Blank))
      )
    }
    "read the returnURL and returnLinkText and YTA header if all present" in {
      model.HostContext.hostContextBinder.bind("anyValName", Map(validReturnUrl, validReturnLinkText, validYTAHeaders)) should contain (
        Right(HostContext(returnUrl = "foo", returnLinkText = "bar", headers = YTA))
      )
    }
    "read the returnURL and returnLinkText and infer Blank if the headers is not present" in {
      model.HostContext.hostContextBinder.bind("anyValName", Map(validReturnUrl, validReturnLinkText)) should contain (
        Right(HostContext(returnUrl = "foo", returnLinkText = "bar", headers = Blank))
      )
    }
    "fail if the returnURL is not present" in {
      model.HostContext.hostContextBinder.bind("anyValName", Map(validReturnLinkText, validBlankHeaders)) should be (Some(Left("No returnUrl query parameter")))
    }
    "fail if the returnLinkText is not present" in {
      model.HostContext.hostContextBinder.bind("anyValName", Map(validReturnUrl, validBlankHeaders)) should be (Some(Left("No returnLinkText query parameter")))
    }
  }
  "Unbinding a host context" should {
    "write out all parameters when headers = Blank" in {
      model.HostContext.hostContextBinder.unbind("anyValName", HostContext(returnUrl = "foo&value", returnLinkText = "bar", headers = Blank)) should be (
        "returnUrl=Wa6yuBSzGvUaibkXblJ8aQ%3D%3D&returnLinkText=w%2FPwaxV%2BKgqutfsU0cyrJQ%3D%3D&headers=47pBbPf0Mz0gcFMRdx8qUQ%3D%3D"
      )
    }
    "write out all parameters when headers = YTA" in {
      model.HostContext.hostContextBinder.unbind("anyValName", HostContext(returnUrl = "foo&value", returnLinkText = "bar", headers = YTA)) should be (
        "returnUrl=Wa6yuBSzGvUaibkXblJ8aQ%3D%3D&returnLinkText=w%2FPwaxV%2BKgqutfsU0cyrJQ%3D%3D&headers=%2BOwkJJim0p%2BX5aV%2BSV2Cew%3D%3D"
      )
    }
  }
}
