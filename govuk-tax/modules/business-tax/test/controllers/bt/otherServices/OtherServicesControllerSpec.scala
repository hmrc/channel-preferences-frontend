package controllers.bt.otherservices

import uk.gov.hmrc.common.BaseSpec
import org.mockito.Mockito._
import play.api.test.Helpers._
import views.helpers.LinkMessage
import views.helpers.RenderableLinkMessage
import controllers.bt.testframework.request.BusinessTaxRequest
import controllers.bt.testframework.fixtures.GeoffFisherTestFixture
import play.api.templates.Html
import play.api.test.{FakeApplication, WithApplication}
import controllers.common.CookieEncryption
import controllers.bt.testframework.mocks.{DateTimeProviderMock, ConnectorMocks, PortalUrlBuilderMock}
import controllers.bt.OtherServicesController
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.domain.User


//abstract class OtherServicesControllerForTest extends WithApplication(FakeApplication()) with CookieEncryption with PortalUrlBuilderMock with ConnectorMocks with DateTimeProviderMock with OtherServicesPageMocks {
//
//  val mockOtherServicesFactory = mock[OtherServicesFactory]
//
//  val controllerUnderTest = new OtherServicesController(mockOtherServicesFactory) with MockedPortalUrlBuilder with MockedConnectors with MockedDateTimeProvider with MockedOtherServicesPages
//}
//
//trait  OtherServicesPageMocks extends MockitoSugar {
//
//  val mockOtherServicesPages = mock[MockableOtherServicesPages]
//
//  trait MockableOtherServicesPages {
//    def otherServicesPage(otherServicesSummary: OtherServicesSummary): Html
//  }
//
//  trait MockedOtherServicesPages {
//    self: OtherServicesController =>
//
//    override private[bt] def otherServicesPage(otherServicesSummary: OtherServicesSummary)(implicit user: User): Html = {
//      mockOtherServicesPages.otherServicesPage(otherServicesSummary)
//    }
//  }
//}

class OtherServicesControllerSpec extends BaseSpec {

  "Calling otherservices with a valid logged in business user" should {

    "render the Other Services template without the Manage your taxes section if the related summary is None" in new OtherServicesControllerForTest with GeoffFisherTestFixture with BusinessTaxRequest {

      val expectedHtml = "<html>some html for other services page</html>"

      val expectedOtherServicesSummary = OtherServicesSummary(
        None,
        OnlineServicesEnrolment(RenderableLinkMessage(LinkMessage("otherServicesEnrolmentURL", "here"))),
        BusinessTaxesRegistration(
          Some(RenderableLinkMessage(LinkMessage("http://www.hmrc.gov.uk/online/nex.htm#2", "Register for SA, CT, employers PAYE or VAT"))),
          RenderableLinkMessage(LinkMessage("http://www.hmrc.gov.uk/online/nex.htm#2", "HMRC website")))
      )

      when(mockOtherServicesFactory.createManageYourTaxes).thenReturn(expectedOtherServicesSummary.manageYourTaxes)
      when(mockOtherServicesFactory.createOnlineServicesEnrolment).thenReturn(expectedOtherServicesSummary.onlineServicesEnrolment)
      when(mockOtherServicesFactory.createBusinessTaxesRegistration).thenReturn(expectedOtherServicesSummary.businessTaxesRegistration)

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

    "render the Other Services template including the three sections if all the summaries are available " in new OtherServicesControllerForTest with GeoffFisherTestFixture with BusinessTaxRequest {

      val expectedHtml = "<html>some html for other services page</html>"

      val expectedOtherServicesSummary = OtherServicesSummary(
        Some(ManageYourTaxes(Seq(RenderableLinkMessage(LinkMessage("http://www.online.hmrc.gov.uk/home/services", "Duty Deferment Electronic Statements (DDES) Service"))))),
        OnlineServicesEnrolment(RenderableLinkMessage(LinkMessage("otherServicesEnrolmentURL", "here"))),
        BusinessTaxesRegistration(
          Some(RenderableLinkMessage(LinkMessage("http://www.hmrc.gov.uk/online/nex.htm#2", "Register for SA, CT, employers PAYE or VAT"))),
          RenderableLinkMessage(LinkMessage("http://www.hmrc.gov.uk/online/nex.htm#2", "HMRC website")))
      )

      when(mockOtherServicesFactory.createManageYourTaxes).thenReturn(expectedOtherServicesSummary.manageYourTaxes)
      when(mockOtherServicesFactory.createOnlineServicesEnrolment).thenReturn(expectedOtherServicesSummary.onlineServicesEnrolment)
      when(mockOtherServicesFactory.createBusinessTaxesRegistration).thenReturn(expectedOtherServicesSummary.businessTaxesRegistration)

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

    "render the Other Services template without the Manage your taxes section if the related summary is empty" in new OtherServicesControllerForTest with GeoffFisherTestFixture with BusinessTaxRequest {
      val expectedHtml = "<html>some html for other services page</html>"

      val expectedOtherServicesSummary = OtherServicesSummary(
        Some(ManageYourTaxes(Seq.empty)),
        OnlineServicesEnrolment(RenderableLinkMessage(LinkMessage("otherServicesEnrolmentURL", "here"))),
        BusinessTaxesRegistration(
          Some(RenderableLinkMessage(LinkMessage("http://www.hmrc.gov.uk/online/nex.htm#2", "Register for SA, CT, employers PAYE or VAT"))),
          RenderableLinkMessage(LinkMessage("http://www.hmrc.gov.uk/online/nex.htm#2", "HMRC website")))
      )

      when(mockOtherServicesFactory.createManageYourTaxes).thenReturn(expectedOtherServicesSummary.manageYourTaxes)
      when(mockOtherServicesFactory.createOnlineServicesEnrolment).thenReturn(expectedOtherServicesSummary.onlineServicesEnrolment)
      when(mockOtherServicesFactory.createBusinessTaxesRegistration).thenReturn(expectedOtherServicesSummary.businessTaxesRegistration)

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
