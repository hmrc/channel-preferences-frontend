/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package views.html.helpers

import org.joda.time._
import org.joda.time.format.{ DateTimeFormat, DateTimeFormatter }

object LastSignInPeriod {

  private def formatter(pattern: String): DateTimeFormatter =
    DateTimeFormat.forPattern(pattern).withZone(DateTimeZone.forID("Europe/London"))

  val succinctFmt = formatter("d MMMM yyy")
  val detailedFmt = formatter("EEEE',' d MMMM yyy 'at' h:mma")

  def succinct(lastLogin: DateTime) = succinctFmt.print(lastLogin)

  def detailed(lastLogin: DateTime) =
    detailedFmt
      .print(lastLogin)
      .replace("AM", "am")
      .replace("PM", "pm")
}
