package model

import play.api.Logger
import play.api.mvc.QueryStringBindable

case class HostContext(returnUrl: String, returnLinkText: String, termsAndConditions: Option[String] = None, email: Option[String] = None)

object HostContext {

  implicit def hostContextBinder(implicit stringBinder: QueryStringBindable[Encrypted[String]]) = new QueryStringBindable[HostContext] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, HostContext]] = {
      val returnUrlResult = stringBinder.bind("returnUrl", params)
      val returnLinkTextResult = stringBinder.bind("returnLinkText", params)
      val termsAndConditionsOptionResult = stringBinder.bind("termsAndConditions", params).liftDecryptedOption
      val emailOptionResult = stringBinder.bind("email", params).liftDecryptedOption

      (returnUrlResult, returnLinkTextResult, termsAndConditionsOptionResult, emailOptionResult) match {
        case (Some(Right(returnUrl)), Some(Right(returnLinkText)), Some("taxCredits"), None) => Some(Left("TaxCredits must provide email"))
        case (Some(Right(returnUrl)), Some(Right(returnLinkText)), terms, email) =>
          Some(Right(HostContext(returnUrl = returnUrl.decryptedValue, returnLinkText = returnLinkText.decryptedValue, termsAndConditions = terms, email = email)))
        case (maybeReturnUrlError, maybeReturnLinkTextError, _, _) =>
          val errorMessage = Seq(
            extractError(maybeReturnUrlError, Some("No returnUrl query parameter")),
            extractError(maybeReturnLinkTextError, Some("No returnLinkText query parameter"))
          ).flatten.mkString("; ")
          Logger.error(errorMessage)
          Some(Left(errorMessage))
      }
    }

    private def extractError(maybeError: Option[Either[String, _]], defaultMessage: Option[String]) = maybeError match {
      case Some(Left(error)) => Some(error)
      case None => defaultMessage
      case _ => None
    }

    override def unbind(key: String, value: HostContext): String = {
      val termsAndEmailString: String = {
            value.termsAndConditions.fold("") { tc => "&" + stringBinder.unbind("termsAndConditions", Encrypted(tc)) +
            value.email.fold("") { em => "&" + stringBinder.unbind("email", Encrypted(em)) }
        }
      }

      stringBinder.unbind("returnUrl", Encrypted(value.returnUrl)) + "&" +
        stringBinder.unbind("returnLinkText", Encrypted(value.returnLinkText)) +
        termsAndEmailString
    }
  }

  implicit class OptionOps(binderResult: Option[Either[String, Encrypted[String]]]) {
    def liftDecryptedOption: Option[String] = binderResult match {
      case Some(Right(encryptedValue)) => Some(encryptedValue.decryptedValue)
      case _ => None
    }
  }
}