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
import views.helpers.{LinkMessage, RenderableLinkMessage}

class CtControllerSpec extends BaseSpec {

    "render the Make a Payment page" in new CtControllerForTest with GeoffFisherTestFixture with BusinessTaxRequest {
      val expectedHtml = "<html>happy Italian pasquetta</html>"
      when(mockPortalUrlBuilder.buildPortalUrl("ctAccountDetails")).thenReturn("ctAccountDetailsUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("btDirectDebits")).thenReturn("btDirectDebitsUrl")

      val expectedAccountDetailsLink = RenderableLinkMessage(LinkMessage(href="ctAccountDetailsUrl", text="NO LINK TEXT DEFINED", sso = true))
      val expectedDirectDebitsLink = RenderableLinkMessage(LinkMessage(href="btDirectDebitsUrl", text="NO LINK TEXT DEFINED", sso = true))
      when(mockCtPages.makeAPaymentPage(expectedAccountDetailsLink, expectedDirectDebitsLink)).thenReturn(Html(expectedHtml))
      val result = Future.successful(controllerUnderTest.makeAPayment(request))
      status(result) shouldBe 200
      contentAsString(result) shouldBe expectedHtml

  }
}