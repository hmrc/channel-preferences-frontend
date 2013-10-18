package controllers.bt.vat

import org.scalatest.mock.MockitoSugar
import play.api.templates.Html
import controllers.bt.VatController
import uk.gov.hmrc.common.microservice.domain.User

trait VatPageMocks extends MockitoSugar {

  val mockVatPages = mock[MockableVatPages]

  trait MockableVatPages {
    def makeAPaymentPage(vatOnlineAccount: String): Html
  }

  trait MockedVatPages {
    self: VatController =>

    private[bt] override def makeAPaymentPage(vatOnlineAccount: String)(implicit user: User): Html = {
      mockVatPages.makeAPaymentPage(vatOnlineAccount)
    }
  }

}