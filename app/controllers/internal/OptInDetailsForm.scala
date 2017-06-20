package controllers.internal

import play.api.data.Form
import play.api.data.Forms._
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
    "accept-tc" -> optional(boolean).verifying("sa_printing_preference.accept_tc_required", _.contains(true)),
    "emailAlreadyStored" -> optional(boolean)
  )(Data.apply)(Data.unapply)
    .verifying("error.email.optIn", _ match {
    case Data((None, _), _, Some(Data.PaperlessChoice.OptedIn), _, _) => false
    case _ => true
  })
    .verifying("email.confirmation.emails.unequal", formData => formData.email._1 == formData.email._2)
  )

  case class Data(email: (Option[String], Option[String]), emailVerified: Option[String], choice: Option[Data.PaperlessChoice], acceptedTCs: Option[Boolean], emailAlreadyStored: Option[Boolean]) {
    lazy val isEmailVerified = emailVerified.contains("true")

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
      Data((emailAddressAsString, emailAddressAsString), None, preference, acceptedTcs, emailAlreadyStored)
    }
  }
}
