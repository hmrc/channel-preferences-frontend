package views.helpers

import views.html.helpers.linkRenderer
import play.api.templates.Html


case class LinkMessage(href: String, text: String)


trait RenderableMessage {
  def render: Html
}

object RenderableMessage {
  implicit def translateStrings(value: String): RenderableStringMessage = RenderableStringMessage(value)
  implicit def translateLinks(link: LinkMessage): RenderableLinkMessage = RenderableLinkMessage(link)
}

case class RenderableStringMessage(value: String) extends RenderableMessage {
  override def render: Html = Html(value)
}

case class RenderableLinkMessage(linkMessage: LinkMessage) extends RenderableMessage {
  override def render: Html = {
    linkRenderer(linkMessage)
  }
}