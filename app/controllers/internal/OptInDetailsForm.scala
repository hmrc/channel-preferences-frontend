package controllers.internal

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import uk.gov.hmrc.emailaddress.EmailAddress


object OptInDetailsForm {
  def apply() = Form[Data](mapping(
    "email" -> tuple(
      "main" -> optional(EmailForm.emailWithLimitedLength),
      "confirm" -> optional(text)
    ),
    "emailVerified" -> optional(text),
    "opt-in" -> optional(boolean).verifying("sa_printing_preference.opt_in_choice_required", _.isDefined).transform(
      _.map(Data.PaperlessChoice.fromBoolean), (p: Option[Data.PaperlessChoice]) => p.map(_.toBoolean)
    ),
    "accept-tc" -> optional(boolean),
    "emailAlreadyStored" -> optional(boolean),
    "newAcceptedTerms" -> tuple(
      "optInTerms" -> optional(boolean),
      "acceptTcTerms" -> optional(boolean)
    ).verifying("sa_printing_preference.accept_tc_required", kvp =>
      (kvp._1.contains(true) && !kvp._2.contains(true))
    )
  )(Data.apply)(Data.unapply)
    .verifying(Constraint.apply[Data](Some("accept-tc"), Nil) { data: Data =>
      data match {
        case Data(_, _, Some(Data.PaperlessChoice.OptedOut), _, _, _) => Valid
        case data =>
          if (data.acceptedTCs.contains(true)) Valid
          else Invalid(ValidationError("sa_printing_preference.accept_tc_required"))
      }
    }).verifying("error.email.optIn", _ match {
    case Data((None, _), _, Some(Data.PaperlessChoice.OptedIn), _, _, _) => false
    case _ => true
  })
    .verifying("email.confirmation.emails.unequal", formData => formData.email._1 == formData.email._2)
  )

  case class Data(email: (Option[String], Option[String]), emailVerified: Option[String], choice: Option[Data.PaperlessChoice], acceptedTCs: Option[Boolean], emailAlreadyStored: Option[Boolean], newAcceptedTerms: (Option[Boolean], Option[Boolean])) {
    lazy val isEmailVerified = emailVerified.contains("true")
    lazy val isEmailAlreadyStored = emailAlreadyStored.contains(true)

    def mainEmail = email._1
  }
  object Data {
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

    def apply(emailAddress: Option[EmailAddress], preference: Option[PaperlessChoice], acceptedTcs: Option[Boolean], emailAlreadyStored: Option[Boolean]): Data = {
      val emailAddressAsString = emailAddress.map(_.value)
      val tuple1: (Option[Boolean], Option[Boolean]) = (preference.map(_.toBoolean), acceptedTcs)
      Data((emailAddressAsString, emailAddressAsString), None, preference, acceptedTcs, emailAlreadyStored, tuple1)
    }
  }
}
