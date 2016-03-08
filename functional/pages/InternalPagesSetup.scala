package pages

import uk.gov.hmrc.endtoend.sa.{RelativeUrl, Page, AbsoluteUrl}

object InternalPagesSetup {
  trait InternalPage extends Page with RelativeUrl
  implicit def internalPageUrls[T <: InternalPage] = AbsoluteUrl.fromRelativeUrl[T](host = "localhost", port = 9000)
}
