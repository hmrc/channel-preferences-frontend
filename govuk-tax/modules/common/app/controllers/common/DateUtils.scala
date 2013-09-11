package controllers.common

import org.joda.time.{ DateTimeZone, DateTime }

trait DateUtils {
  def currentDate = new DateTime(DateTimeZone.UTC)
}
