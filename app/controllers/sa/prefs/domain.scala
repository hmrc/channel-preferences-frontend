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
    case OptIn => Some(true)
    case OptOut => Some(false)
  }
}
object EmailPreference {
  def fromBoolean(b: Option[Boolean]): EmailPreference = b match {
    case Some(true) => OptIn
    case Some(false) => OptOut
  }
}
case object OptIn extends EmailPreference
case object OptOut extends EmailPreference

case class EmailFormDataWithPreference(email: (String, Option[String]), emailVerified: Option[String], preference: EmailPreference) {
  lazy val isEmailVerified = emailVerified == Some("true")

  def mainEmail = email._1
}
