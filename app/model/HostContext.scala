package model

import controllers.ExternalUrls
import play.api.mvc.QueryStringBindable
import play.twirl.api.{Html, HtmlFormat}

case class HostContext(returnUrl: String, returnLinkText: String, headers: HostContext.Headers)

object HostContext {
  implicit def hostContextBinder(implicit stringBinder: QueryStringBindable[Encrypted[String]]) = new QueryStringBindable[HostContext] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, HostContext]] =
      for {
        errorOrReturnUrl      <- stringBinder.bind("returnUrl", params)
        errorOrReturnLinkText <- stringBinder.bind("returnLinkText", params)
        errorOrHeaders        <- stringBinder.bind("headers", params) orElse Some(Right(Encrypted(Headers.Blank.name)))
      } yield (errorOrReturnUrl, errorOrReturnLinkText, errorOrHeaders) match {
        case (Right(returnUrl), Right(returnLinkText), Right(headersName)) =>
          Right(HostContext(returnUrl = returnUrl.decryptedValue, returnLinkText = returnLinkText.decryptedValue, headers = Headers(headersName.decryptedValue)))
        case someErrors => Left(Seq(
          someErrors._1.left.toOption,
          someErrors._2.left.toOption,
          someErrors._3.left.toOption
        ).mkString(", "))
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
      val navLinks = HtmlFormat.empty
      val navTitle = ""
    }
    def apply(name: String) = name match {
      case YTA.name => YTA
      case Blank.name => Blank
    }
  }

  // TODO remove once YTA no longer depends on this
  lazy val defaultsForYtaManageAccountPages = HostContext(returnUrl = ExternalUrls.manageAccount, returnLinkText = "Go to manage account", headers = Headers.YTA)
  lazy val defaultsForYtaLoginPages = HostContext(returnUrl = ExternalUrls.businessTaxHome, returnLinkText = "Go to your tax account", headers = Headers.Blank)
}