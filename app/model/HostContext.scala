package model

import play.api.Logger
import play.api.mvc.QueryStringBindable

case class HostContext(returnUrl: String, returnLinkText: String)

object HostContext {

  implicit def hostContextBinder(implicit stringBinder: QueryStringBindable[Encrypted[String]]) = new QueryStringBindable[HostContext] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, HostContext]] =
      (stringBinder.bind("returnUrl", params), stringBinder.bind("returnLinkText", params)) match {
        case (Some(Right(returnUrl)), Some(Right(returnLinkText))) =>
          Some(Right(HostContext(returnUrl = returnUrl.decryptedValue, returnLinkText = returnLinkText.decryptedValue)))
        case (maybeReturnUrlError, maybeReturnLinkTextError) =>
          val errorMessage = Seq(
            extractError(maybeReturnUrlError, Some("No returnUrl query parameter")),
            extractError(maybeReturnLinkTextError, Some("No returnLinkText query parameter"))
          ).flatten.mkString("; ")
          Logger.error(errorMessage)
          Some(Left(errorMessage))
      }

    private def extractError(maybeError: Option[Either[String, _]], defaultMessage: Option[String]) = maybeError match {
      case Some(Left(error)) => Some(error)
      case None => defaultMessage
      case _ => None
    }

    override def unbind(key: String, value: HostContext): String =
      stringBinder.unbind("returnUrl",      Encrypted(value.returnUrl))      + "&" +
      stringBinder.unbind("returnLinkText", Encrypted(value.returnLinkText))
  }
}