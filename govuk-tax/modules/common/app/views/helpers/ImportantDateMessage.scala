package views.helpers

import play.api.templates.Html
import views.html.helpers.importantDateRenderer

case class ImportantDateMessage(date: RenderableDateMessage, text: String, startDate: RenderableDateMessage, endDate: RenderableDateMessage, link: Option[RenderableLinkMessage])
                                (implicit val postMessageText: Option[String] = if (!link.isDefined) Some("ct.message.importantDates.returnAlreadyReceived") else None) extends RenderableMessage {
                                   def render: Html = importantDateRenderer(this)
                                 }
