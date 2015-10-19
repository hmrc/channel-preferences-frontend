package hostcontext

import org.scalatest.{Matchers, WordSpec}

class HostContextSpec extends WordSpec with Matchers {
  "Binding a host context" should {
    "read the returnURL and returnLinkText if both present" in {
      HostContext.hostContextBinder.bind("anyValName", Map("returnUrl" -> Seq("foo"), "returnLinkText" -> Seq("bar"))) should contain (Right(HostContext(returnUrl = "foo", returnLinkText = "bar")))
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
      HostContext.hostContextBinder.unbind("anyValName", HostContext(returnUrl = "foo&value", returnLinkText = "bar")) should be ("returnUrl=foo%26value&returnLinkText=bar")
    }
  }
}
