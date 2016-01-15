package model

import controllers.ExternalUrls
import play.api.Logger
import play.api.mvc.QueryStringBindable
import play.twirl.api.Html

case class HostContext(returnUrl: String, returnLinkText: String, headers: HostContext.Headers)

object HostContext {
  implicit def hostContextBinder(implicit stringBinder: QueryStringBindable[Encrypted[String]]) = new QueryStringBindable[HostContext] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, HostContext]] =
      (stringBinder.bind("returnUrl", params), stringBinder.bind("returnLinkText", params), stringBinder.bind("headers", params) orElse Some(Right(Encrypted(Headers.Blank.name)))) match {
        case (Some(Right(returnUrl)), Some(Right(returnLinkText)), Some(Right(headersName))) =>
          Some(Right(HostContext(returnUrl = returnUrl.decryptedValue, returnLinkText = returnLinkText.decryptedValue, headers = Headers(headersName.decryptedValue))))
        case (maybeReturnUrlError, maybeReturnLinkTextError, maybeHeaderError) =>
          val errorMessage = Seq(
            extractError(maybeReturnUrlError, Some("No returnUrl query parameter")),
            extractError(maybeReturnLinkTextError, Some("No returnLinkText query parameter")),
            extractError(maybeHeaderError, None)
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
      stringBinder.unbind("returnLinkText", Encrypted(value.returnLinkText)) + "&" +
      stringBinder.unbind("headers",        Encrypted(value.headers.name))
  }

  sealed trait Headers {
    def name: String
    def navLinks: Html
    def navTitle: String
  }
  object Headers {
    case object YTA extends Headers {
      val name = "yta"
      val navTitle = "Your tax account"
      lazy val navLinks = views.html.includes.yta_header_nav_links()
    }
    case object Blank extends Headers {
      val name = "blank"
      val navTitle = ""
      lazy val navLinks = views.html.includes.header_nav_links()
    }
    def apply(name: String) = name match {
      case YTA.name => YTA
      case Blank.name => Blank
    }
  }

  // TODO remove once YTA no longer depends on this
  lazy val defaultsForYtaManageAccountPages = HostContext(returnUrl = ExternalUrls.manageAccount, returnLinkText = "Go to manage account", headers = Headers.YTA)
  lazy val defaultsForYtaWarningsPartial = HostContext(returnUrl = ExternalUrls.manageAccount, returnLinkText = "Manage account", headers = Headers.YTA)
  lazy val defaultsForYtaLoginPages = HostContext(returnUrl = ExternalUrls.businessTaxHome, returnLinkText = "Go to your tax account", headers = Headers.Blank)
}