package controllers.bt.otherservices

import org.scalatest.mock.MockitoSugar
import play.api.templates.Html
import play.api.templates.Html
import uk.gov.hmrc.common.microservice.domain.User
import controllers.bt.OtherServicesController
import org.scalatest.mock.MockitoSugar

trait  OtherServicesPageMocks extends MockitoSugar {

  val mockOtherServicesPages = mock[MockableOtherServicesPages]

  trait MockableOtherServicesPages {
    def otherServicesPage(otherServicesSummary: OtherServicesSummary): Html
  }

  trait MockedOtherServicesPages {
    self: OtherServicesController =>

    override private[bt] def otherServicesPage(otherServicesSummary: OtherServicesSummary)(implicit user: User): Html = {
      mockOtherServicesPages.otherServicesPage(otherServicesSummary)
    }
  }
}
