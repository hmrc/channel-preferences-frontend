package views.helpers

import views.html.helpers.linkRenderer
import play.api.templates.Html
import org.joda.time.LocalDate
import controllers.common.domain.accountSummaryDateFormatter


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

case class RenderableDateMessage(date: LocalDate) extends RenderableMessage {
  val formattedDate = accountSummaryDateFormatter.format(date)
  override def render: Html = Html(formattedDate)
}