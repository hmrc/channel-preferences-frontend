package controllers.sa.prefs

import uk.gov.hmrc.domain.Email

case class EmailFormData(email: (String, Option[String]), emailVerified: Option[String]) {
  lazy val isEmailVerified = emailVerified == Some("true")

  def mainEmail = email._1
}
object EmailFormData {
  def apply(emailAddress: Option[Email]): EmailFormData = {
    EmailFormData(emailAddress.map(e => (e.value, Some(e.value))).getOrElse(("", None)), None)
  }
}

sealed trait EmailPreference {
  def toBoolean = this match {
    case OptIn => true
    case OptOut => false
  }
}
object EmailPreference {
  def fromBoolean(b: Boolean): EmailPreference = if (b) OptIn else OptOut
}
case object OptIn extends EmailPreference
case object OptOut extends EmailPreference

case class EmailFormDataWithPreference(email: (Option[String], Option[String]), emailVerified: Option[String], preference: Option[EmailPreference]) {
  lazy val isEmailVerified = emailVerified == Some("true")

  def mainEmail = email._1
}
object EmailFormDataWithPreference {
  def apply(emailAddress: Option[Email], preference: Option[EmailPreference]): EmailFormDataWithPreference = {
    val emailAddressAsString = emailAddress.map(_.value)
    EmailFormDataWithPreference((emailAddressAsString, emailAddressAsString), None, preference)
  }
}
