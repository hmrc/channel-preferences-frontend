package helpers

import hostcontext.HostContext

object TestFixtures {
  val sampleHostContext = HostContext(
    returnUrl = "someReturnUrl",
    returnLinkText = "someReturnLinkText",
    headers = HostContext.Headers.Blank
  )
}
