package controllers.bt


import uk.gov.hmrc.common.BaseSpec
import controllers.common.CookieEncryption
import org.mockito.Mockito._
import play.api.mvc.Result
import play.api.test.Helpers._
import controllers.bt.spechelpers.{GeoffFisherVatExpectations, WithVatApplication}
import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, User}
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import org.scalatest.mock.MockitoSugar

class VatControllerSpec extends BaseSpec with CookieEncryption {

  "Calling makeAPayment with a valid logged in business user" should {

      "render the Make a Payment landing page" in new WithVatApplication with GeoffFisherVatExpectations {
        val expectedHtml = "<html>happy Canadian thanksgiving</html>"
        when (expectations.makeAPayment("vatOnlineAccount")).thenReturn(expectedHtml)
        val result: Result = vatController.makeAPayment(request)
        status(result) shouldBe 200
        contentAsString(result) shouldBe expectedHtml
      }
    }
  }





