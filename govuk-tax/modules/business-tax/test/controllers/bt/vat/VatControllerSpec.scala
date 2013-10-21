package controllers.bt.vat

import uk.gov.hmrc.common.BaseSpec
import org.mockito.Mockito._
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.templates.Html
import controllers.bt.testframework.fixtures.GeoffFisherTestFixture
import controllers.bt.testframework.request.BusinessTaxRequest

class VatControllerSpec extends BaseSpec {

  "Calling makeAPayment with a valid logged in business user" should {

    "render the Make a Payment landing page" in new VatControllerForTest with GeoffFisherTestFixture with BusinessTaxRequest {
      val expectedHtml = "<html>happy Canadian thanksgiving</html>"
      when(mockPortalUrlBuilder.buildPortalUrl("vatOnlineAccount")).thenReturn("vatOnlineAccountUrl")
      when(mockVatPages.makeAPaymentPage("vatOnlineAccountUrl")).thenReturn(Html(expectedHtml))
      val result: Result = controllerUnderTest.makeAPayment(request)
      status(result) shouldBe 200
      contentAsString(result) shouldBe expectedHtml
    }
  }
}