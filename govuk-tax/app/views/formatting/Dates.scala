package views.formatting

import org.joda.time.format.{ ISODateTimeFormat, DateTimeFormat }
import org.joda.time.{ DateTime, LocalDate, DateTimeZone }

object Dates {

  private[formatting] val dateFormat = DateTimeFormat.forPattern("MMMM d, yyyy").withZone(DateTimeZone.forID("Europe/London"))
  private[formatting] val shortDateFormat = DateTimeFormat.forPattern("yyyy-MM-dd").withZone(DateTimeZone.forID("Europe/London"))
  private[formatting] val easyReadingTimestampFormat = DateTimeFormat.forPattern("EEEE MMMM d, yyyy 'at' h:mmaa").withZone(DateTimeZone.forID("Europe/London"))
  private[formatting] val portalCompatibleTimestampFormat = ISODateTimeFormat.dateTime.withZoneUTC

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
  def parseShortDate(str: String) = shortDateFormat.parseLocalDate(str)

  def portalCompatibleTimestamp = portalCompatibleTimestampFormat.print(new DateTime)
}
