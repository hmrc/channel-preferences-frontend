package hostcontext

import controllers.sa.prefs.ExternalUrls
import play.api.mvc.QueryStringBindable

case class HostContext(returnUrl: String, returnLinkText: String) {
  override def toString = returnUrl
}
object HostContext {
  implicit def hostContextBinder(implicit stringBinder: QueryStringBindable[String]) = new QueryStringBindable[HostContext] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, HostContext]] =
      for {
        errorOrReturnUrl      <- stringBinder.bind("returnUrl", params)
        errorOrReturnLinkText <- stringBinder.bind("returnLinkText", params)
      } yield (errorOrReturnUrl, errorOrReturnLinkText) match {
        case (Right(returnUrl), Right(returnLinkText)) => Right(HostContext(returnUrl = returnUrl, returnLinkText = returnLinkText))
        case _                                         => Left("could not parse URL")
      }
    override def unbind(key: String, value: HostContext): String =
      stringBinder.unbind("returnUrl", value.returnUrl) + "&" + stringBinder.unbind("returnLinkText", value.returnLinkText)
  }

  // TODO remove once YTA no longer depends on this
  lazy val defaultsForYta = HostContext(returnUrl = ExternalUrls.businessTaxHome, returnLinkText = "Go to your tax account")
}