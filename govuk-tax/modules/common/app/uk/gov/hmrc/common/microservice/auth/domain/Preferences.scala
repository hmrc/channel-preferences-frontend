package uk.gov.hmrc.common.microservice.auth.domain

case class Preferences(sa: Option[Notification])

case class Notification(digital: Option[Boolean], email: Option[Email])

case class Email(value: String) {
  @inline final private val simpleEmailEx = """\b[a-zA-Z0-9.!#$%&’*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*\b""".r
  require(simpleEmailEx.findFirstIn(value) != None)

  override def toString: String = value
}

object Email {

  implicit def optionStringToOptionEmail(email: Option[String]): Option[Email] = apply(email)

  def apply(email: Option[String]): Option[Email] = email match {
    case Some(e) => Some(new Email(e))
    case _ => None
  }
}