/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers.internal

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.emailaddress.EmailAddress


object OptInDetailsForm {
  def apply() = Form[Data](mapping(
    "email" -> tuple(
      "main" -> optional(EmailForm.emailWithLimitedLength).verifying("error.email.optIn", _.nonEmpty),
      "confirm" -> optional(text)
    ),
    "emailVerified" -> optional(text),
    "opt-in" -> optional(boolean).verifying("sa_printing_preference.opt_in_choice_required", _.isDefined).transform(
      _.map(PaperlessChoice.fromBoolean), (p: Option[PaperlessChoice]) => p.map(_.toBoolean)
    ),
    "accept-tc" -> optional(boolean).verifying("sa_printing_preference.accept_tc_required", _.contains(true)),
    "emailAlreadyStored" -> optional(boolean)
  )(Data.apply)(Data.unapply)
    .verifying("error.email.optIn", _ match {
      case Data((None, _), _, Some(PaperlessChoice.OptedIn), _, _) => false
      case _ => true
    })
  )

  case class Data(email: (Option[String], Option[String]), emailVerified: Option[String], choice: Option[PaperlessChoice], acceptedTCs: Option[Boolean], emailAlreadyStored: Option[Boolean]) {
    lazy val isEmailVerified = emailVerified.contains("true")
    lazy val isEmailAlreadyStored = emailAlreadyStored.contains(true)

    def mainEmail: Option[String] = email._1
  }
  object Data {

    def apply(emailAddress: Option[EmailAddress], preference: Option[PaperlessChoice], acceptedTcs: Option[Boolean], emailAlreadyStored: Option[Boolean]): Data = {
      val emailAddressAsString = emailAddress.map(_.value)
      Data((emailAddressAsString, emailAddressAsString), None, preference, acceptedTcs, emailAlreadyStored)
    }
  }
}

object ReOptInDetailsForm {
  def apply() = Form[Data](mapping(
    "email" -> tuple(
      "main" -> optional(EmailForm.emailWithLimitedLength).verifying("error.email.optIn", _.nonEmpty),
      "confirm" -> optional(text)
    ),
    "emailVerified" -> optional(text),
    "opt-in" -> optional(boolean).verifying("sa_printing_preference.opt_in_choice_required", _.isDefined).transform(
      _.map(PaperlessChoice.fromBoolean), (p: Option[PaperlessChoice]) => p.map(_.toBoolean)
    ),
    "accept-tc" -> optional(boolean).verifying("sa_printing_preference.accept_tc_required", _.contains(true)),
    "emailAlreadyStored" -> optional(boolean)
  )(Data.apply)(Data.unapply)
    .verifying("error.email.optIn", _ match {
      case Data((None, _), _, Some(PaperlessChoice.OptedIn), _, _) => false
      case _ => true
    })
  )

  case class Data(email: (Option[String], Option[String]), emailVerified: Option[String], choice: Option[PaperlessChoice], acceptedTCs: Option[Boolean], emailAlreadyStored: Option[Boolean]) {
    lazy val isEmailVerified = emailVerified.contains("true")
    lazy val isEmailAlreadyStored = emailAlreadyStored.contains(true)

    def mainEmail: Option[String] = email._1
  }
  object Data {

    def apply(emailAddress: Option[EmailAddress], preference: Option[PaperlessChoice], acceptedTcs: Option[Boolean], emailAlreadyStored: Option[Boolean]): Data = {
      val emailAddressAsString = emailAddress.map(_.value)
      Data((emailAddressAsString, emailAddressAsString), None, preference, acceptedTcs, emailAlreadyStored)
    }
  }
}

object OptInTaxCreditsDetailsForm {

  def apply() = Form[Data](mapping(
    "email" -> tuple(
      "main" -> optional(EmailForm.emailWithLimitedLength),
      "confirm" -> optional(text)
    ),
    "emailVerified" -> optional(text),
    "emailAlreadyStored" -> optional(boolean),
    "termsAndConditions" -> tuple(
      "opt-in" -> optional(boolean).verifying("tc_printing_preference.opt_in_choice_required", _.isDefined).transform(
        _.map(PaperlessChoice.fromBoolean), (p: Option[PaperlessChoice]) => p.map(_.toBoolean)
      ),
      "accept-tc" -> optional(boolean).verifying("tc_printing_preference.accept_tc_required", _.contains(true))
    ).transform[(Option[Boolean],Option[Boolean])](
      { case (p: Option[PaperlessChoice], acceptTc: Option[Boolean]) => (p.map(_.toBoolean), acceptTc) },
      { case (paperlessChoice: Option[Boolean], acceptTc: Option[Boolean]) => (paperlessChoice.map(PaperlessChoice.fromBoolean(_)), acceptTc) }
    ).verifying("tc_printing_preference.accept_tc_required", kvp =>
      ((kvp._1.contains(true) && kvp._2.contains(true)) || !kvp._1.contains(true))
    )
  )(Data.apply)(Data.unapply)
    .verifying("email.confirmation.emails.unequal", formData => formData.email._1 == formData.email._2)
  )

  case class Data(email: (Option[String], Option[String]), emailVerified: Option[String], emailAlreadyStored: Option[Boolean], termsAndConditions: (Option[Boolean], Option[Boolean])) {
    lazy val isEmailVerified = emailVerified.contains("true")
    lazy val isEmailAlreadyStored = emailAlreadyStored.contains(true)

    def mainEmail = email._1
  }
  object Data {

    def apply(emailAddress: Option[EmailAddress], termsAndConditions: (Option[PaperlessChoice], Option[Boolean]), emailAlreadyStored: Option[Boolean]): Data = {
      val emailAddressAsString = emailAddress.map(_.value)
      val optedInAsBoolean = termsAndConditions._1.map(_.toBoolean)
      Data((emailAddressAsString, emailAddressAsString), None, emailAlreadyStored, (optedInAsBoolean, termsAndConditions._2))
    }
  }
}

sealed trait PaperlessChoice {
  def toBoolean = this match {
    case PaperlessChoice.OptedIn => true
    case PaperlessChoice.OptedOut => false
  }
}

object PaperlessChoice {
  case object OptedIn extends PaperlessChoice
  case object OptedOut extends PaperlessChoice

  def fromBoolean(b: Boolean): PaperlessChoice = if (b) PaperlessChoice.OptedIn else PaperlessChoice.OptedOut
}
