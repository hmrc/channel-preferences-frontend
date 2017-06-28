package helpers

import model.HostContext

object TestFixtures {
  val sampleHostContext = HostContext(
    returnUrl = "someReturnUrl",
    returnLinkText = "someReturnLinkText"
  )

  def taxCreditsHostContext(email: String) = HostContext(
    returnUrl = "someReturnUrl",
    returnLinkText = "someReturnLinkText",
    termsAndConditions = Some("taxCredits"),
    email = Some(email)
  )

}
