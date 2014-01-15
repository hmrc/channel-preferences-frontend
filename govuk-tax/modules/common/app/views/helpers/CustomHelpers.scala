package views.helpers

import views.html.helper.FieldConstructor
import views.html.helpers.simpleFieldConstructor
import java.text.DateFormatSymbols

object CustomHelpers {

  implicit val myFields = FieldConstructor(simpleFieldConstructor.f)

}

object DateFormatSymbols {

  val months = new DateFormatSymbols().getMonths()

  val monthsWithIndexes = months.zipWithIndex.take(12).map{case (s, i) => ((i+1).toString, s)}.toSeq
}