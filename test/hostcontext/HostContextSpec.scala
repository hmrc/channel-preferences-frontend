package hostcontext

import org.scalatest.{Matchers, WordSpec}
import uk.gov.hmrc.play.test.WithFakeApplication

class HostContextSpec extends WordSpec with Matchers with WithFakeApplication {
  "Binding a host context" should {
    "read the returnURL and returnLinkText if both present" in {
      HostContext.hostContextBinder.bind("anyValName", Map("returnUrl" -> Seq("9tNUeRTIYBD0RO+T5WRO7A]=="), "returnLinkText" -> Seq("w/PwaxV+KgqutfsU0cyrJQ=="))) should contain (Right(HostContext(returnUrl = "foo", returnLinkText = "bar")))
    }
    "fail if the returnURL is not present" in {
      HostContext.hostContextBinder.bind("anyValName", Map("other" -> Seq("foo"), "returnLinkText" -> Seq("bar"))) should be (None)
    }
    "fail if the returnLinkText is not present" in {
      HostContext.hostContextBinder.bind("anyValName", Map("other" -> Seq("foo"), "returnUrl" -> Seq("bar"))) should be (None)
    }
  }
  "Unbinding a host context" should {
    "write out all parameters" in {
      HostContext.hostContextBinder.unbind("anyValName", HostContext(returnUrl = "foo&value", returnLinkText = "bar")) should be ("returnUrl=Wa6yuBSzGvUaibkXblJ8aQ%3D%3D&returnLinkText=w%2FPwaxV%2BKgqutfsU0cyrJQ%3D%3D")
    }
  }
}
