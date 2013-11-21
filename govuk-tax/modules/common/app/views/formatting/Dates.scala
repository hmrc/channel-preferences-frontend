package views.formatting

import org.joda.time.format.DateTimeFormat
import org.joda.time.{ DateTime, LocalDate, DateTimeZone }

object Dates {

  private[formatting] val dateFormat = DateTimeFormat.forPattern("d MMMM y").withZone(DateTimeZone.forID("Europe/London"))
  private[formatting] val shortDateFormat = DateTimeFormat.forPattern("yyyy-MM-dd").withZone(DateTimeZone.forID("Europe/London"))
  private[formatting] val easyReadingDateFormat = DateTimeFormat.forPattern("EEEE d MMMM yyyy").withZone(DateTimeZone.forID("Europe/London"))
  private[formatting] val easyReadingTimestampFormat = DateTimeFormat.forPattern("h:mmaa").withZone(DateTimeZone.forID("Europe/London"))

  def formatDate(date: LocalDate) = dateFormat.print(date)

  def formatDate(date: Option[LocalDate], default: String) = date match {
    case Some(d) => dateFormat.print(d)
    case None => default
  }

  def formatDateTime(date: DateTime) = dateFormat.print(date)

  def formatEasyReadingTimestamp(date: Option[DateTime], default: String) = date match {
    case Some(d) => {
      s"${easyReadingDateFormat.print(d)} at ${easyReadingTimestampFormat.print(d).toLowerCase}"
    }
    case None => default
  }

  def shortDate(date: LocalDate) = shortDateFormat.print(date)

}
