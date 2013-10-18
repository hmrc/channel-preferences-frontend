package controllers.bt

import uk.gov.hmrc.common.BaseSpec
import controllers.bt.spechelpers.{GeoffFisherExpectations, WithBusinessTaxApplication}
import org.mockito.Mockito._
import play.api.test.Helpers._
import views.helpers.LinkMessage
import views.helpers.RenderableLinkMessage
import controllers.bt.otherServices.{OtherServicesSummary, BusinessTaxesRegistration, OnlineServicesEnrolment}
import scala.Some
import controllers.common.CookieEncryption


class OtherServicesControllerSpec extends BaseSpec with CookieEncryption {

  "Calling otherServices with a valid logged in business user" should {
    "render the Other Services template including the Enrol to use a new online service section" in new WithBusinessTaxApplication with GeoffFisherExpectations {

      val expectedHtml = "<html>some html for other services page</html>"

      val expectedOtherServicesSummary = OtherServicesSummary(
        OnlineServicesEnrolment(RenderableLinkMessage(LinkMessage("otherServicesEnrolmentURL", "here"))),
        BusinessTaxesRegistration(
          Some(RenderableLinkMessage(LinkMessage("http://www.hmrc.gov.uk/online/nex.htm#2", "Register for SA, CT, employers PAYE or VAT"))),
          RenderableLinkMessage(LinkMessage("http://www.hmrc.gov.uk/online/nex.htm#2", "HMRC website")))
      )

      when(mockOtherServicesFactory.create(geoffFisherUser)).thenReturn(expectedOtherServicesSummary)

      when(expectations.buildPortalUrl(geoffFisherUser, request, "otherServicesEnrolment")).thenReturn("otherServicesEnrolmentURL")

      val otherServicesEnrolment = OnlineServicesEnrolment(RenderableLinkMessage(LinkMessage("otherServicesEnrolmentURL", "here")))
      val businessTaxesRegistration = BusinessTaxesRegistration(
        Some(RenderableLinkMessage(LinkMessage("http://www.hmrc.gov.uk/online/nex.htm#2", "Register for SA, CT, employers PAYE or VAT"))),
        RenderableLinkMessage(LinkMessage("http://www.hmrc.gov.uk/online/nex.htm#2", "HMRC website"))
      )

      when(expectations.otherServicesPage(geoffFisherUser, expectedOtherServicesSummary)).thenReturn(expectedHtml)

      val result = otherServicesController.otherServices(request)

      status(result) shouldBe 200
      contentAsString(result) shouldBe expectedHtml
    }
  }


}
