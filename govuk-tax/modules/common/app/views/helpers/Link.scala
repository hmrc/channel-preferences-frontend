package views.helpers

import play.api.templates.{Html, HtmlFormat}
import play.api.i18n.Messages


object PortalLink {
  def apply(url: String) = Link(url, None, sso = HasSso)
}

object InternalLink {
  def apply(url: String) = Link(url, None)
}

object ExternalLink {
  def apply(url: String) = Link(url, None, target = NewWindow)
}

trait Target {
  protected val targetName: String
  val toAttr = Link.attr("target", targetName)
}
case object SameWindow extends Target {
  override val targetName = "_self"
}
case object NewWindow extends Target {
  override val targetName = "_blank"
}

trait PossibleSso {
  protected val value: String
  val toAttr = Link.attr("data-sso", value)
}
case object NoSso extends PossibleSso {
  override val value = "false"
}
case object HasSso extends PossibleSso {
  override val value = "true"
}

case class Link(url: String,
                value: Option[String],
                id: Option[String] = None,
                target: Target = SameWindow,
                sso: PossibleSso = NoSso,
                cssClasses: Option[String] = None) {

  import Link._

  private def hrefAttr = attr("href", url)
  private def idAttr = id.map(attr("id", _)).getOrElse("")
  private def text = value.map(v => escape(Messages(v))).getOrElse("")
  private def cssAttr = cssClasses.map(attr("class", _)).getOrElse("")

  def toHtml = Html(s"<a$idAttr$hrefAttr${target.toAttr}${sso.toAttr}$cssAttr>$text</a>")
}

object Link {
  private def escape(str: String) = HtmlFormat.escape(str).toString()

  def attr(name: String, value: String) = s""" $name="${escape(value)}""""
}