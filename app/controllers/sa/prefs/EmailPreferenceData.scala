package controllers.sa.prefs

import uk.gov.hmrc.domain.Email

case class EmailPreferenceData(email: (String, Option[String]), emailVerified: Option[String]) {
  lazy val isEmailVerified = emailVerified == Some("true")

  def mainEmail = email._1
}
object EmailPreferenceData {
  def apply(emailAddress: Option[Email]): EmailPreferenceData = {
    EmailPreferenceData(emailAddress.map(e => (e.value, Some(e.value))).getOrElse(("", None)), None)
  }
}