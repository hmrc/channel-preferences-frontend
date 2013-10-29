package views.helpers

import views.html.helpers.{moneyPoundsRenderer, linkRenderer}
import play.api.templates.Html
import org.joda.time.LocalDate
import controllers.common.domain.accountSummaryDateFormatter
import views.helpers.RenderableMessageProperty.RenderableMessageProperty


case class LinkMessage(href: String, text: String, id: Option[String] = None,
                       newWindow: Boolean = false, postLinkText: Option[String] = None, sso: Boolean)

case class HrefKey(key: String)

object LinkMessage {

  def externalLink(hrefKey: HrefKey, text: String, id: Option[String] = None, postLinkText: Option[String] = None) =
    LinkMessage(getPath(hrefKey.key), text, id, newWindow = true, postLinkText = postLinkText, sso = false)

  def internalLink(href: String, text: String, id: Option[String] = None, postLinkText: Option[String] = None) =
    LinkMessage(href, text, id, newWindow = false, postLinkText = postLinkText, sso = false)

  def portalLink(href: String, text: Option[String] = None, id: Option[String] = None, postLinkText: Option[String] = None) =
    LinkMessage(href, text.getOrElse("NO LINK TEXT DEFINED"), id, newWindow = false, postLinkText = postLinkText, sso = true)

  private def getPath(pathKey: String): String = {
    import play.api.Play
    import play.api.Play.current
    lazy val env = Play.mode
    s"${Play.configuration.getString(s"govuk-tax.$env.externalLinks.$pathKey").getOrElse("")}"
  }
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

case class RenderableLinkMessage(linkMessage: LinkMessage) extends RenderableMessage {
  override def render: Html = {
    linkRenderer(linkMessage)
  }

  override def set(property: (RenderableMessageProperty, String)): RenderableLinkMessage = {
    import RenderableMessageProperty.Link._
    property match {
      case (ID, idValue) => linkMessage.copy(id = Some(idValue))
      case (TEXT, textValue) => linkMessage.copy(text = textValue)
      case _ => this
    }

  }

}

case class RenderableDateMessage(date: LocalDate) extends RenderableMessage {
  val formattedDate = accountSummaryDateFormatter.format(date)

  override def render: Html = Html(formattedDate)
}

case class RenderableMoneyMessage(moneyPounds: MoneyPounds) extends RenderableMessage {
  override def render: Html = {
    moneyPoundsRenderer(moneyPounds)
  }
}