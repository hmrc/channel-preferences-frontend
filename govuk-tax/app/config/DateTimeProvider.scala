package config

import org.joda.time.{ DateTimeZone, DateTime }

trait DateTimeProvider {
  def now: () => DateTime = () => DateTime.now(DateTimeZone.UTC)
}