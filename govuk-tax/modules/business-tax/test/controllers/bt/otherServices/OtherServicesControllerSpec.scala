package controllers.bt.otherservices

import uk.gov.hmrc.common.BaseSpec
import org.mockito.Mockito._
import play.api.test.Helpers._
import views.helpers.LinkMessage
import views.helpers.RenderableLinkMessage
import controllers.common.CookieEncryption
import controllers.bt.testframework.request.BusinessTaxRequest
import controllers.bt.testframework.fixtures.GeoffFisherTestFixture
import play.api.templates.Html


class OtherServicesControllerSpec extends BaseSpec with CookieEncryption {

  "Calling otherservices with a valid logged in business user" should {
    "render the Other Services template including the Enrol to use a new online service section" in new OtherServicesControllerForTest with GeoffFisherTestFixture with BusinessTaxRequest {

      val expectedHtml = "<html>some html for other services page</html>"

      val expectedOtherServicesSummary = OtherServicesSummary(
        OnlineServicesEnrolment(RenderableLinkMessage(LinkMessage("otherServicesEnrolmentURL", "here"))),
        BusinessTaxesRegistration(
          Some(RenderableLinkMessage(LinkMessage("http://www.hmrc.gov.uk/online/nex.htm#2", "Register for SA, CT, employers PAYE or VAT"))),
          RenderableLinkMessage(LinkMessage("http://www.hmrc.gov.uk/online/nex.htm#2", "HMRC website")))
      )

      when(mockOtherServicesFactory.create).thenReturn(expectedOtherServicesSummary)
      when(mockPortalUrlBuilder.buildPortalUrl("otherServicesEnrolment")).thenReturn("otherServicesEnrolmentURL")

      val otherServicesEnrolment = OnlineServicesEnrolment(RenderableLinkMessage(LinkMessage("otherServicesEnrolmentURL", "here")))

      val businessTaxesRegistration = BusinessTaxesRegistration(
        Some(RenderableLinkMessage(LinkMessage("http://www.hmrc.gov.uk/online/nex.htm#2", "Register for SA, CT, employers PAYE or VAT"))),
        RenderableLinkMessage(LinkMessage("http://www.hmrc.gov.uk/online/nex.htm#2", "HMRC website"))
      )

      when(mockOtherServicesPages.otherServicesPage(expectedOtherServicesSummary)).thenReturn(Html(expectedHtml))

      val result = controllerUnderTest.otherServices(request)

      status(result) shouldBe 200
      contentAsString(result) shouldBe expectedHtml
    }
  }
}
