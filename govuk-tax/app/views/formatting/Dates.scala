package views.formatting

import org.joda.time.format.DateTimeFormat
import org.joda.time.{ DateTime, LocalDate }

object Dates {

  private[formatting] val dateFormat = DateTimeFormat.forPattern("MMMM d, yyyy")
  private[formatting] val shortDateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")
  private[formatting] val easyReadingTimestampFormat = DateTimeFormat.forPattern("EEEE MMMM d, yyyy 'at' hh:mm aa")

  def formatDate(date: LocalDate) = dateFormat.print(date)

  def formatDate(date: Option[LocalDate], default: String) = date match {
    case Some(d) => dateFormat.print(d)
    case None => default
  }

  def formatEasyReadingTimestamp(date: Option[DateTime], default: String) = date match {
    case Some(d) => easyReadingTimestampFormat.print(d)
    case None => default
  }

  def shortDate(date: LocalDate) = shortDateFormat.print(date)
}
