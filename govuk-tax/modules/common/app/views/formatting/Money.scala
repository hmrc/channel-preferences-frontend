package views.formatting

import views.helpers.{MoneyPounds, RenderableMoneyMessage}
import play.api.templates.Html

object Money {

  def pounds(value: BigDecimal, decimalPlaces: Int = 0) : Html = RenderableMoneyMessage(MoneyPounds(value, decimalPlaces)).render
}
