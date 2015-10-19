package hostcontext

import org.scalatest.{Matchers, WordSpec}

class HostContextSpec extends WordSpec with Matchers {
  "Binding a host context" should {
    "read the returnURL if present" in {
      HostContext.hostContextBinder.bind("anyValName", Map("returnUrl" -> Seq("blah"))) should contain (Right(HostContext(returnUrl = "blah")))
    }
    "fail if the returnURL is not present" in {
      HostContext.hostContextBinder.bind("anyValName", Map("other" -> Seq("blah"))) should be (None)
    }
  }
  "Unbinding a host context" should {
    "read the returnURL if present" in {
      HostContext.hostContextBinder.unbind("anyValName", HostContext(returnUrl = "blah")) should be ("returnUrl=blah")
    }
  }
}
