/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package helpers

import controllers.internal.ReOptInPage10
import model.HostContext

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
