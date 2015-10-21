package hostcontext

import hostcontext.HostContext.Headers.{YTA, Blank}
import org.scalatest.{Matchers, WordSpec}
import uk.gov.hmrc.play.test.WithFakeApplication

class HostContextSpec extends WordSpec with Matchers with WithFakeApplication {
  "Binding a host context" should {
    "read the returnURL and returnLinkText and Blank header if all present" in {
      HostContext.hostContextBinder.bind("anyValName", Map("returnUrl" -> Seq("9tNUeRTIYBD0RO+T5WRO7A]=="), "returnLinkText" -> Seq("w/PwaxV+KgqutfsU0cyrJQ=="), "headers" -> Seq("47pBbPf0Mz0gcFMRdx8qUQ=="))) should contain (
        Right(HostContext(returnUrl = "foo", returnLinkText = "bar", headers = Blank))
      )
    }
    "read the returnURL and returnLinkText and YTA header if all present" in {
      HostContext.hostContextBinder.bind("anyValName", Map("returnUrl" -> Seq("9tNUeRTIYBD0RO+T5WRO7A]=="), "returnLinkText" -> Seq("w/PwaxV+KgqutfsU0cyrJQ=="), "headers" -> Seq("+OwkJJim0p+X5aV+SV2Cew=="))) should contain (
        Right(HostContext(returnUrl = "foo", returnLinkText = "bar", headers = YTA))
      )
    }
    "fail if the returnURL is not present" in {
      HostContext.hostContextBinder.bind("anyValName", Map("other" -> Seq("foo"), "returnLinkText" -> Seq("bar"))) should be (None)
    }
    "fail if the returnLinkText is not present" in {
      HostContext.hostContextBinder.bind("anyValName", Map("other" -> Seq("foo"), "returnUrl" -> Seq("bar"))) should be (None)
    }
  }
  "Unbinding a host context" should {
    "write out all parameters when headers = Blank" in {
      HostContext.hostContextBinder.unbind("anyValName", HostContext(returnUrl = "foo&value", returnLinkText = "bar", headers = Blank)) should be (
        "returnUrl=Wa6yuBSzGvUaibkXblJ8aQ%3D%3D&returnLinkText=w%2FPwaxV%2BKgqutfsU0cyrJQ%3D%3D&headers=47pBbPf0Mz0gcFMRdx8qUQ%3D%3D"
      )
    }
    "write out all parameters when headers = YTA" in {
      HostContext.hostContextBinder.unbind("anyValName", HostContext(returnUrl = "foo&value", returnLinkText = "bar", headers = YTA)) should be (
        "returnUrl=Wa6yuBSzGvUaibkXblJ8aQ%3D%3D&returnLinkText=w%2FPwaxV%2BKgqutfsU0cyrJQ%3D%3D&headers=%2BOwkJJim0p%2BX5aV%2BSV2Cew%3D%3D"
      )
    }
  }
}
