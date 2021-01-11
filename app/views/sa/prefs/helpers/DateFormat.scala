/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package views.sa.prefs.helpers

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.twirl.api.Html

object DateFormat {

  private val longDateFormatter = DateTimeFormat.forPattern("d MMMM yyyy")

  def longDateFormat(date: Option[LocalDate]) = date.map(d => Html(longDateFormatter.print(d)))
}
