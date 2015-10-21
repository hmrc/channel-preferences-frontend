package hostcontext

import controllers.sa.prefs.{Encrypted, ExternalUrls}
import play.api.mvc.QueryStringBindable

case class HostContext(returnUrl: String, returnLinkText: String)

object HostContext {
  implicit def hostContextBinder(implicit stringBinder: QueryStringBindable[Encrypted[String]]) = new QueryStringBindable[HostContext] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, HostContext]] =
      for {
        errorOrReturnUrl      <- stringBinder.bind("returnUrl", params)
        errorOrReturnLinkText <- stringBinder.bind("returnLinkText", params)
      } yield (errorOrReturnUrl, errorOrReturnLinkText) match {
        case (Right(returnUrl), Right(returnLinkText)) => Right(HostContext(returnUrl = returnUrl.decryptedValue, returnLinkText = returnLinkText.decryptedValue))
        case (Left(error1), Left(error2))              => Left(error1 + "," + error2)
        case (Left(error1), _)                         => Left(error1)
        case (_, Left(error2))                         => Left(error2)
      }
    override def unbind(key: String, value: HostContext): String =
      stringBinder.unbind("returnUrl", Encrypted(value.returnUrl)) + "&" + stringBinder.unbind("returnLinkText", Encrypted(value.returnLinkText))
  }

  // TODO remove once YTA no longer depends on this
  lazy val defaultsForYta = HostContext(returnUrl = ExternalUrls.businessTaxHome, returnLinkText = "Go to your tax account")
}