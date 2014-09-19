package controllers.sa.prefs.internal

object EmailOptInJourney extends Enumeration {
  type Journey = Value
  val Interstitial, AccountDetails = Value
}
