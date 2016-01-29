package pages

import uk.gov.hmrc.endtoend.sa.{Page, ToAbsoluteUrl}

object InternalPagesSetup {
  trait InternalPage extends Page
  implicit def internalPageUrls[T <: InternalPage] = ToAbsoluteUrl.fromRelativeUrl[T](host = "localhost", port = 9000)
}
