package views.helpers

import views.html.helpers.{link, moneyPoundsRenderer}
import play.api.templates.Html
import org.joda.time.LocalDate
import views.helpers.RenderableMessageProperty.RenderableMessageProperty
import java.text.{SimpleDateFormat, DateFormat}


@deprecated("use Link")
case class LinkMessage(href: String, text: String, id: String,
                       newWindow: Boolean = false, sso: Boolean)

object LinkMessage {

  def internalLink(href: String, text: String, id: String) =
    LinkMessage(href, text, id, newWindow = false, sso = false)

  def portalLink(href: String, text: String, id: String) =
    LinkMessage(href, text, id, newWindow = false, sso = true)

}

case class MoneyPounds(value: BigDecimal, decimalPlaces: Int = 2, roundUp: Boolean = false) {

  def isNegative = value < 0

  def quantity = s"%,.${decimalPlaces}f".format(value.setScale(decimalPlaces, if (roundUp) BigDecimal.RoundingMode.CEILING else BigDecimal.RoundingMode.FLOOR).abs)
}

object RenderableMessageProperty extends Enumeration {
  type RenderableMessageProperty = Value

  object Link {
    val ID, TEXT = Value
  }
}

trait RenderableMessage {
  
  def render: Html

  def set(property: (RenderableMessageProperty, String)): RenderableMessage = this
}


object RenderableMessage {
  implicit def translateStrings(value: String): RenderableStringMessage = RenderableStringMessage(value)

  implicit def translateLinks(link: LinkMessage): RenderableLinkMessage = RenderableLinkMessage(link)

  implicit def translateMoneyPounds(money: MoneyPounds): RenderableMoneyMessage = RenderableMoneyMessage(money)

  implicit def translateDate(date: LocalDate): RenderableDateMessage = RenderableDateMessage(date)
}

case class RenderableStringMessage(value: String) extends RenderableMessage {
  override def render: Html = Html(value)
}

@deprecated("use helper.link")
case class RenderableLinkMessage(linkMessage: LinkMessage) extends RenderableMessage {
  override def render: Html = {
    link(linkMessage.id, linkMessage.href, linkMessage.text, linkMessage.newWindow, linkMessage.sso)
  }
}

case class RenderableDateMessage(date: LocalDate)(implicit dateFormat: DateFormat = new SimpleDateFormat("d MMM yyy")) extends RenderableMessage {
  val formattedDate = dateFormat.format(date.toDate)

  override def render: Html = Html(formattedDate)
}

case class RenderableMoneyMessage(moneyPounds: MoneyPounds) extends RenderableMessage {
  override def render: Html = {
    moneyPoundsRenderer(moneyPounds)
  }
}