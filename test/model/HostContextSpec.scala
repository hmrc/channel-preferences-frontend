package model

import org.scalatest.{Matchers, WordSpec}
import uk.gov.hmrc.play.test.WithFakeApplication

class HostContextSpec extends WordSpec with Matchers with WithFakeApplication {

  "Binding a host context" should {
    val validReturnUrl = "returnUrl" -> Seq("9tNUeRTIYBD0RO+T5WRO7A]==")
    val validReturnLinkText = "returnLinkText" -> Seq("w/PwaxV+KgqutfsU0cyrJQ==")

    "read the returnURL and returnLinkText if both present" in {
      model.HostContext.hostContextBinder.bind("anyValName", Map(validReturnUrl, validReturnLinkText)) should contain (
        Right(HostContext(returnUrl = "foo", returnLinkText = "bar"))
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
