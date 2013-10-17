package controllers.bt

import uk.gov.hmrc.common.BaseSpec
import controllers.common.CookieEncryption
import org.mockito.Mockito._
import play.api.mvc.Result
import play.api.test.Helpers._
import controllers.bt.regimeViews.AccountSummaries
import org.mockito.Matchers
import controllers.bt.spechelpers.{GeoffFisherExpectations, WithBusinessTaxApplication}
import views.helpers.{RenderableLinkMessage, LinkMessage}
import controllers.bt.otherServices.OtherServicesEnrolment

class BusinessTaxControllerSpec extends BaseSpec with CookieEncryption {

  "Calling makeAPaymentLanding with a valid logged in business user" should {

    "render the Make a Payment landing page" in new WithBusinessTaxApplication with GeoffFisherExpectations {

      val expectedHtml = "<html>some html for landing page</html>"

      when (expectations.makeAPaymentLandingPage(geoffFisherUser)).thenReturn(expectedHtml)

      val result: Result = businessTaxController.makeAPaymentLanding(request)

      status(result) shouldBe 200
      contentAsString(result) shouldBe expectedHtml
    }
  }

  "Calling otherServices with a valid logged in business user" should {
    "render the Other Services template including the Enrol to use a new online service section" in new WithBusinessTaxApplication with GeoffFisherExpectations {

      val expectedHtml = "<html>some html for other services page</html>"

      when (expectations.buildPortalUrl(geoffFisherUser, request, "otherServicesEnrolment")).thenReturn("otherServicesEnrolmentURL")

      when(expectations.otherServicesPage(geoffFisherUser, OtherServicesEnrolment(RenderableLinkMessage(LinkMessage("otherServicesEnrolmentURL", "here"))))).thenReturn(expectedHtml)

      val result = businessTaxController.otherServices(request)

      status(result) shouldBe 200
      contentAsString(result) shouldBe expectedHtml
    }
  }

  "Calling home with a valid logged in business user" should {

    "pass the correct data to the home page for a user in all regimes" in new WithBusinessTaxApplication with GeoffFisherExpectations {

      val geoffFisherSummaries = mock[AccountSummaries]
      val expectedHtml = "<html>some html for the Business Tax Homepage</html>"

      when (expectations.buildPortalUrl(geoffFisherUser, request, "home")).thenReturn("homeURL")

      when (expectations.businessTaxHomepage(geoffFisherUser, "homeURL", geoffFisherSummaries)).thenReturn(expectedHtml)

      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(geoffFisherUser))).thenReturn(geoffFisherSummaries)

      val result: Result = businessTaxController.home(request)

      status(result) shouldBe 200
      contentAsString(result) shouldBe expectedHtml
    }
  }
}

