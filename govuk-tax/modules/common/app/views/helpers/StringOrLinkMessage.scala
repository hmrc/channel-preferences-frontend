package views.helpers

trait StringOrLinkMessage
case class LinkMessage(href: String, text: String) extends StringOrLinkMessage
case class StringMessage(value: String) extends StringOrLinkMessage {
  override def toString = value
}