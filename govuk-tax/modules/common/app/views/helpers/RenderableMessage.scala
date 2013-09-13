package views.helpers

import views.html.helpers.linkRenderer
import play.api.templates.Html


case class LinkMessage(href: String, text: String)


trait RenderableMessage {
  def render: Html
}

case class RenderableStringMessage(value: String) extends RenderableMessage {
  override def render: Html = Html(value)
}

case class RenderableLinkMessage(linkMessage: LinkMessage) extends RenderableMessage {
  override def render: Html = {
    linkRenderer(linkMessage)
  }
}