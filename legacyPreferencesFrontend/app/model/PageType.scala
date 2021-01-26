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

package model
import enumeratum.{ Enum, EnumEntry, PlayJsonEnum }

sealed abstract class PageType() extends EnumEntry

case object PageType extends Enum[PageType] with PlayJsonEnum[PageType] {

  val values = findValues

  case object IPage extends PageType

  case object TCPage extends PageType

  case object ReOptInPage extends PageType

  case object AndroidOptInPage extends PageType

  case object AndroidReOptInPage extends PageType

  case object AndroidOptOutPage extends PageType

  case object AndroidReOptOutPage extends PageType

  case object IosOptInPage extends PageType

  case object IosReOptInPage extends PageType

  case object IosOptOutPage extends PageType

  case object IosReOptOutPage extends PageType

  case object CYSConfirmPage extends PageType
}
