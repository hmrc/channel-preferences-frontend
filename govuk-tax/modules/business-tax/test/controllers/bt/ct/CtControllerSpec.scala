package controllers.bt.ct

import org.mockito.Mockito._
import uk.gov.hmrc.common.BaseSpec
import controllers.bt.testframework.fixtures.GeoffFisherTestFixture
import controllers.bt.testframework.request.BusinessTaxRequest
import play.api.templates.Html
import play.api.mvc.Result
import play.api.test.Helpers._
import org.mockito.Matchers
import concurrent.Future

class CtControllerSpec extends BaseSpec {

    "render the Make a Payment page" in new CtControllerForTest with GeoffFisherTestFixture with BusinessTaxRequest {
      val expectedHtml = "<html>happy Italian pasquetta</html>"
      when(mockPortalUrlBuilder.buildPortalUrl("ctAccountDetails")).thenReturn("ctAccountDetailsUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("ctDirectDebits")).thenReturn("ctDirectDebitsUrl")
      when(mockCtPages.makeAPaymentPage("ctAccountDetailsUrl", "ctDirectDebitsUrl")).thenReturn(Html(expectedHtml))
      val result = Future.successful(controllerUnderTest.makeAPayment(request))
      status(result) shouldBe 200
      contentAsString(result) shouldBe expectedHtml

  }
}