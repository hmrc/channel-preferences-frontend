package views.helpers


trait Link {
  val url: String

  val id: Option[String] = None
  val newWindow: Boolean = false
  val sso: Boolean = false
  val cssClasses: Option[String] = None
}

case class PortalLink(override val url: String) extends Link {
  override val sso = true
}

case class InternalLink(override val url: String) extends Link

case class ExternalLink(override val url: String) extends Link {
  override val newWindow = true
}

case class CustomLink(override val id: Option[String],
                      override val url: String,
                      override val newWindow: Boolean,
                      override val sso: Boolean,
                      override val cssClasses: Option[String]) extends Link
