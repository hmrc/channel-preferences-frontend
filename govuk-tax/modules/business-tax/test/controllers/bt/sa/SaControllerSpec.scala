package controllers.bt.sa

import org.mockito.Mockito._
import uk.gov.hmrc.common.BaseSpec
import controllers.bt.testframework.fixtures.GeoffFisherTestFixture
import controllers.bt.testframework.request.BusinessTaxRequest
import play.api.templates.Html
import play.api.test.Helpers._
import concurrent.Future
import views.helpers.{LinkMessage, RenderableLinkMessage}

class SaControllerSpec extends BaseSpec {

    "render the Make a Payment page" in new SaControllerForTest with GeoffFisherTestFixture with BusinessTaxRequest {
      val expectedHtml = "<html>happy Italian pasquetta</html>"
      when(mockPortalUrlBuilder.buildPortalUrl("btDirectDebits")).thenReturn("saDirectDebitsUrl")

      val expectedDirectDebitsLink = RenderableLinkMessage(LinkMessage(href="saDirectDebitsUrl", text="NO LINK TEXT DEFINED", sso = true))
      when(mockSaPages.makeAPaymentPage(expectedDirectDebitsLink, user.getSa.utr.utr)).thenReturn(Html(expectedHtml))
      val result = Future.successful(controllerUnderTest.makeAPayment(request))
      status(result) shouldBe 200
      contentAsString(result) shouldBe expectedHtml

  }
}
