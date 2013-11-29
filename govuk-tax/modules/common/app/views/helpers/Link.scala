package views.helpers


trait Link{
  val url: String
  val newWindow: Boolean
  val sso: Boolean
}

case class PortalLink(override val url: String) extends Link {
  override val newWindow = false
  override val sso = true
}

case class InternalLink(override val url: String) extends Link {
  override val newWindow = false
  override val sso = false
}

case class ExternalLink(override val url: String) extends Link {
  override val newWindow = true
  override val sso = false
}
