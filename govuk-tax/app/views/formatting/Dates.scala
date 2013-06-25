package views.formatting

import org.joda.time.format.DateTimeFormat
import org.joda.time.LocalDate

object Dates {

  private[formatting] val dateFormat = DateTimeFormat.forPattern("MMMM d, yyyy")

  def formatDate(date: LocalDate) = dateFormat.print(date)

  def formatDate(date: Option[LocalDate], default: String) = date match {
    case Some(d) => dateFormat.print(d)
    case None => default
  }
}
