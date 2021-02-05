/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
