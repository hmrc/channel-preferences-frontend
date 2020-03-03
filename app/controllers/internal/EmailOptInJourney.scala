/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers.internal

object EmailOptInJourney extends Enumeration {
  type Journey = Value
  val Interstitial, AccountDetails = Value
}
