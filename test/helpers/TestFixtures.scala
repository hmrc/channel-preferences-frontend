package helpers

import model.HostContext

object TestFixtures {
  val sampleHostContext = HostContext(
    returnUrl = "someReturnUrl",
    returnLinkText = "someReturnLinkText"
  )
}
