package views.formatting

import org.joda.time.format.DateTimeFormat
import org.joda.time.LocalDate

object Dates {

  private[formatting] val dateFormat = DateTimeFormat.forPattern("MMMM d, yyyy")
  private[formatting] val shortDateFormat = DateTimeFormat.forPattern("dd/MM/yy")

  def formatDate(date: LocalDate) = dateFormat.print(date)

  def formatDate(date: Option[LocalDate], default: String) = date match {
    case Some(d) => dateFormat.print(d)
    case None => default
  }

  def shortDate(date: LocalDate) = shortDateFormat.print(date)
}
