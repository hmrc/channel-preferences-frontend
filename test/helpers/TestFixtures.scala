/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package helpers

import model.HostContext

import controllers.internal.ReOptInPage10

object TestFixtures {
  val sampleHostContext = HostContext(
    returnUrl = "someReturnUrl",
    returnLinkText = "someReturnLinkText"
  )

  def alreadyOptedInUrlHostContext = HostContext(
    returnUrl = "someReturnUrl",
    returnLinkText = "someReturnLinkText",
    alreadyOptedInUrl = Some("someAlreadyOptedInUrl")
  )

  def taxCreditsHostContext(email: String) = HostContext(
    returnUrl = "someReturnUrl",
    returnLinkText = "someReturnLinkText",
    termsAndConditions = Some("taxCredits"),
    email = Some(email)
  )

  def reOptInHostContext(email: String) = HostContext(
    returnUrl = "someReturnUrl",
    returnLinkText = "someReturnLinkText",
    email = Some(email),
    cohort = Some(ReOptInPage10)
  )

  def reOptInHostContext() = HostContext(
    returnUrl = "someReturnUrl",
    returnLinkText = "someReturnLinkText",
    cohort = Some(ReOptInPage10)
  )

}
