package controllers.bt.vat

import uk.gov.hmrc.common.BaseSpec
import org.mockito.Mockito._
import play.api.test.Helpers._
import play.api.templates.Html
import controllers.bt.testframework.fixtures.GeoffFisherTestFixture
import controllers.bt.testframework.request.BusinessTaxRequest
import views.helpers.{LinkMessage, RenderableLinkMessage}

class VatControllerSpec extends BaseSpec {

  "Calling makeAPayment with a valid logged in business user" should {

    "render the Make a Payment landing page" in new VatControllerForTest with GeoffFisherTestFixture with BusinessTaxRequest {
      val expectedHtml = "<html>happy Canadian thanksgiving</html>"
      when(mockPortalUrlBuilder.buildPortalUrl("vatOnlineAccount")).thenReturn("vatOnlineAccountUrl")
      val expectedVatOnlineAccountLink = RenderableLinkMessage(LinkMessage(href = "vatOnlineAccountUrl", text = "NO LINK TEXT DEFINED",  sso = true))
      when(mockVatPages.makeAPaymentPage(expectedVatOnlineAccountLink)).thenReturn(Html(expectedHtml))
      val result = controllerUnderTest.makeAPayment(request)
      status(result) shouldBe 200
      contentAsString(result) shouldBe expectedHtml
    }
  }
}