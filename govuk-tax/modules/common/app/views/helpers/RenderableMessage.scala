package views.helpers

import views.html.helpers.moneyPoundsRenderer
import play.api.templates.Html
import org.joda.time.LocalDate
import views.helpers.RenderableMessageProperty.RenderableMessageProperty
import java.text.{SimpleDateFormat, DateFormat}


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

  implicit def translateMoneyPounds(money: MoneyPounds): RenderableMoneyMessage = RenderableMoneyMessage(money)

  implicit def translateDate(date: LocalDate): RenderableDateMessage = RenderableDateMessage(date)
}

case class RenderableStringMessage(value: String) extends RenderableMessage {
  override def render: Html = Html(value)
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