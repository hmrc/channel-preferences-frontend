package controllers.bt.businesstax

import uk.gov.hmrc.common.BaseSpec
import org.mockito.Mockito._
import play.api.mvc.Result
import play.api.test.Helpers._
import controllers.bt.accountsummary.AccountSummaries
import controllers.bt.testframework.fixtures.GeoffFisherTestFixture
import controllers.bt.testframework.request.BusinessTaxRequest
import play.api.templates.Html
import org.mockito.Matchers

class BusinessTaxControllerSpec extends BaseSpec {

  "Calling makeAPaymentLanding with a valid logged in business user" should {

    "render the Make a Payment landing page" in new BusinessTaxControllerForTest with GeoffFisherTestFixture with BusinessTaxRequest {

      val expectedHtml = "<html>some html for landing page</html>"

      when (mockBusinessTaxPages.makeAPaymentLandingPage()).thenReturn(Html(expectedHtml))

      val result: Result = controllerUnderTest.makeAPaymentLanding(request)

      status(result) shouldBe 200
      contentAsString(result) shouldBe expectedHtml
    }
  }

  "Calling home with a valid logged in business user" should {

    "pass the correct data to the home page for a user in all regimes" in new BusinessTaxControllerForTest with GeoffFisherTestFixture with BusinessTaxRequest {

      val geoffFisherSummaries = mock[AccountSummaries]
      val expectedHtml = "<html>some html for the Business Tax Homepage</html>"

      when(mockPortalUrlBuilder.buildPortalUrl("home")).thenReturn("homeURL")
      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(user))).thenReturn(geoffFisherSummaries)
      when(mockBusinessTaxPages.businessTaxHomepage("homeURL", geoffFisherSummaries)).thenReturn(Html(expectedHtml))

      val result: Result = controllerUnderTest.home(request)

      status(result) shouldBe 200
      contentAsString(result) shouldBe expectedHtml
    }
  }
}
