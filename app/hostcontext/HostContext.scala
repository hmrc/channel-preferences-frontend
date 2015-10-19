package hostcontext

import play.api.mvc.QueryStringBindable

case class HostContext(returnUrl: String) {
  override def toString = returnUrl
}
object HostContext {
  implicit def hostContextBinder(implicit stringBinder: QueryStringBindable[String]) = new QueryStringBindable[HostContext] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, HostContext]] =
      for {
        errorOrReturnUrl <- stringBinder.bind("returnUrl", params)
      } yield errorOrReturnUrl match {
        case Right(returnUrl) => Right(HostContext(returnUrl))
        case _                => Left("returnUrl parameter missing")
      }
    override def unbind(key: String, value: HostContext): String = stringBinder.unbind("returnUrl", value.returnUrl)
  }
}