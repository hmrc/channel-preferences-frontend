package views.helpers

import play.api.templates.{Html, HtmlFormat}
import play.api.i18n.Messages


object PortalLink {
  def apply(url: String) = Link(url, None, sso = true)
}

object InternalLink {
  def apply(url: String) = Link(url, None)
}

object ExternalLink {
  def apply(url: String) = Link(url, None, newWindow = true)
}

case class Link(url: String,
                value: Option[String],
                id: Option[String] = None,
                newWindow: Boolean = false,
                sso: Boolean = false,
                cssClasses: Option[String] = None) {

  import Link._

  private def hrefAttr = attr("href", url)
  private def idAttr = id.map(attr("id", _)).getOrElse("")
  private def targetAttr = if (newWindow) attr("target", "_blank") else attr("target", "_self")
  private def ssoAttr = attr("data-sso", String.valueOf(sso))
  private def text = value.map(v => escape(Messages(v))).getOrElse("")
  private def cssAttr = cssClasses.map(attr("class", _)).getOrElse("")

  def toHtml = Html(s"<a$idAttr$hrefAttr$targetAttr$ssoAttr$cssAttr>$text</a>")
}

object Link {
  private def escape(str: String) = HtmlFormat.escape(str).toString()

  private def attr(name: String, value: String) = s""" $name="${escape(value)}""""
}