/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package service
import connectors.{ EmailPreference, PreferenceResponse, TermsAndConditonsAcceptance }
import model.StatusName.{ Alright, BouncedEmail, EmailNotVerified, NewCustomer, NoEmail, Paper }
import org.scalatest.{ MustMatchers, WordSpec }

class PreferenceBasedStatusServiceSpec extends WordSpec with MustMatchers {

  private val preferenceStatusService = new PreferencesBasedStatusService()

  "determineStatus" should {
    "return Paper for a preference with not accepted general terms and conditions" in {
      preferenceStatusService.determineStatus(preferences(termsAccepted = false)) mustBe Paper
    }

    "return NewCustomer for a customer who doesn't have any preferences settings" in {
      preferenceStatusService.determineStatus(preference = None) mustBe NewCustomer
    }

    "return Bounced when preferences has bounces" in {
      preferenceStatusService.determineStatus(preferences(hasBounces = true)) mustBe BouncedEmail
    }

    "return Alright for preferences with accepted general terms and conditions" in {
      preferenceStatusService.determineStatus(preferences()) mustBe Alright
    }

    "return EmailNotVerified for a preference that is not verified" in {
      preferenceStatusService.determineStatus(preferences(isVerified = false)) mustBe EmailNotVerified
    }

    "return NoEmail for a preference with accepted general terms and conditions but no email" in {
      preferenceStatusService.determineStatus(preferences(containsEmail = false)) mustBe NoEmail
    }

  }

  private def preferences(
    isVerified: Boolean = true,
    hasBounces: Boolean = false,
    termsAccepted: Boolean = true,
    containsEmail: Boolean = true
  ) =
    Some(
      PreferenceResponse(
        Map("generic" -> TermsAndConditonsAcceptance(termsAccepted)),
        if (!containsEmail)
          None
        else
          Some(
            EmailPreference(
              "pihklyljtgoxeoh@mail.com",
              isVerified = isVerified,
              hasBounces = hasBounces,
              mailboxFull = false,
              linkSent = None
            )
          )
      )
    )
}
