package controllers.bt.otherservices

import uk.gov.hmrc.common.BaseSpec
import org.mockito.Mockito._
import play.api.test.Helpers._
import views.helpers.LinkMessage
import views.helpers.RenderableLinkMessage
import org.mockito.Matchers
import play.api.test.{FakeRequest, FakeApplication, WithApplication}
import controllers.bt.testframework.mocks.PortalUrlBuilderMock
import controllers.bt.OtherServicesController
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, User}
import uk.gov.hmrc.common.microservice.auth.domain.{Regimes, UserAuthority}
import scala.concurrent.Future
import org.jsoup.Jsoup

class OtherServicesControllerSpec extends BaseSpec with MockitoSugar {

  "Calling otherservices with a valid logged in business user" should {

    "render the Other Services template without the Manage your taxes section if the related summary is None" in new WithApplication(FakeApplication()) with PortalUrlBuilderMock {

      val mockOtherServicesFactory = mock[OtherServicesFactory]
      val controllerUnderTest = new OtherServicesController(mockOtherServicesFactory, null)(null)

      val saRoot = Some(SaRoot(SaUtr("sa-utr"), Map.empty[String, String]))
      val user = User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(sa = saRoot), decryptedToken = None)
      val request = FakeRequest()

      val expectedOtherServicesSummary = OtherServicesSummary(
        None,
        OnlineServicesEnrolment(RenderableLinkMessage(LinkMessage("otherServicesEnrolmentURL", "here", sso = true))),
        OnlineServicesEnrolment(RenderableLinkMessage(LinkMessage("otherServicesDeEnrolmentURL", "here", sso = true))),
        BusinessTaxesRegistration(
          Some(RenderableLinkMessage(LinkMessage("http://www.hmrc.gov.uk/online/nex.htm#2", "Register for SA, CT, employers PAYE or VAT", sso = true))),
          RenderableLinkMessage(LinkMessage("http://www.hmrc.gov.uk/online/nex.htm#2", "HMRC website", sso = true)))
      )

      when(mockOtherServicesFactory.createManageYourTaxes(Matchers.any())(Matchers.eq(user), Matchers.any())).thenReturn(expectedOtherServicesSummary.manageYourTaxes)

      when(mockOtherServicesFactory.createOnlineServicesEnrolment(Matchers.any())).thenReturn(expectedOtherServicesSummary.onlineServicesEnrolment)

      when(mockOtherServicesFactory.createOnlineServicesDeEnrolment(Matchers.any())).thenReturn(expectedOtherServicesSummary.onlineServicesDeEnrolment)

      when(mockOtherServicesFactory.createBusinessTaxesRegistration(Matchers.any())(org.mockito.Matchers.eq(user))).thenReturn(expectedOtherServicesSummary.businessTaxesRegistration)

      val otherServicesEnrolment = OnlineServicesEnrolment(RenderableLinkMessage(LinkMessage("otherServicesEnrolmentURL", "here", sso = false)))

      val businessTaxesRegistration = BusinessTaxesRegistration(
        Some(RenderableLinkMessage(LinkMessage(href = "http://www.hmrc.gov.uk/online/nex.htm#2", text = "Register for SA, CT, employers PAYE or VAT", sso = true))),
        RenderableLinkMessage(LinkMessage("http://www.hmrc.gov.uk/online/nex.htm#2", "HMRC website", sso = true))
      )

      val result = Future.successful(controllerUnderTest.otherServicesPage(user, request))

      status(result) shouldBe 200

      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("manageYourTaxesSection") shouldBe null
    }

    "render the Other Services template including the three sections if all the summaries are available " in new WithApplication(FakeApplication()) with PortalUrlBuilderMock {

      val mockOtherServicesFactory = mock[OtherServicesFactory]
      val controllerUnderTest = new OtherServicesController(mockOtherServicesFactory, null)(null)

      val saRoot = Some(SaRoot(SaUtr("sa-utr"), Map.empty[String, String]))
      val user = User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(sa = saRoot), decryptedToken = None)
      val request = FakeRequest()

      val expectedOtherServicesSummary = OtherServicesSummary(
        Some(ManageYourTaxes(Seq(RenderableLinkMessage(LinkMessage("http://www.online.hmrc.gov.uk/home/services", "Duty Deferment Electronic Statements (DDES) Service", sso = true))))),
        OnlineServicesEnrolment(RenderableLinkMessage(LinkMessage("otherServicesEnrolmentURL", "here", sso = true))),
        OnlineServicesEnrolment(RenderableLinkMessage(LinkMessage("otherServicesDeEnrolmentURL", "here", sso = true))),
        BusinessTaxesRegistration(
          Some(RenderableLinkMessage(LinkMessage("http://www.hmrc.gov.uk/online/nex.htm#2", "Register for SA, CT, employers PAYE or VAT", sso = true))),
          RenderableLinkMessage(LinkMessage("http://www.hmrc.gov.uk/online/nex.htm#2", "HMRC website", sso = true)))
      )

      when(mockOtherServicesFactory.createManageYourTaxes(Matchers.any())(Matchers.eq(user), Matchers.any())).thenReturn(expectedOtherServicesSummary.manageYourTaxes)
      when(mockOtherServicesFactory.createOnlineServicesEnrolment(Matchers.any())).thenReturn(expectedOtherServicesSummary.onlineServicesEnrolment)
      when(mockOtherServicesFactory.createOnlineServicesDeEnrolment(Matchers.any())).thenReturn(expectedOtherServicesSummary.onlineServicesDeEnrolment)
      when(mockOtherServicesFactory.createBusinessTaxesRegistration(Matchers.any())(org.mockito.Matchers.eq(user))).thenReturn(expectedOtherServicesSummary.businessTaxesRegistration)

      val otherServicesEnrolment = OnlineServicesEnrolment(RenderableLinkMessage(LinkMessage("otherServicesEnrolmentURL", "here", sso = false)))

      val businessTaxesRegistration = BusinessTaxesRegistration(
        Some(RenderableLinkMessage(LinkMessage("http://www.hmrc.gov.uk/online/nex.htm#2", "Register for SA, CT, employers PAYE or VAT", sso = false))),
        RenderableLinkMessage(LinkMessage("http://www.hmrc.gov.uk/online/nex.htm#2", "HMRC website", sso = false))
      )

      val result = Future.successful(controllerUnderTest.otherServicesPage(user, request))

      status(result) shouldBe 200

      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("manageYourTaxesSection") should not be null
      document.getElementById("availableServices") should not be null
      document.getElementById("noOtherServices") shouldBe null
    }

    "render the Other Services template without the Manage your taxes section if the related summary is empty" in new WithApplication(FakeApplication()) with PortalUrlBuilderMock {
      val mockOtherServicesFactory = mock[OtherServicesFactory]
      val controllerUnderTest = new OtherServicesController(mockOtherServicesFactory, null)(null)

      val saRoot = Some(SaRoot(SaUtr("sa-utr"), Map.empty[String, String]))
      val user = User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(sa = saRoot), decryptedToken = None)
      val request = FakeRequest()

      val expectedOtherServicesSummary = OtherServicesSummary(
        Some(ManageYourTaxes(Seq.empty)),
        OnlineServicesEnrolment(RenderableLinkMessage(LinkMessage("otherServicesEnrolmentURL", "here", sso = true))),
        OnlineServicesEnrolment(RenderableLinkMessage(LinkMessage("otherServicesDeEnrolmentURL", "here", sso = true))),
        BusinessTaxesRegistration(
          Some(RenderableLinkMessage(LinkMessage("http://www.hmrc.gov.uk/online/nex.htm#2", "Register for SA, CT, employers PAYE or VAT", sso = true))),
          RenderableLinkMessage(LinkMessage("http://www.hmrc.gov.uk/online/nex.htm#2", "HMRC website", sso = true)))
      )

      when(mockOtherServicesFactory.createManageYourTaxes(Matchers.any())(Matchers.eq(user), Matchers.any())).thenReturn(expectedOtherServicesSummary.manageYourTaxes)
      when(mockOtherServicesFactory.createOnlineServicesEnrolment(Matchers.any())).thenReturn(expectedOtherServicesSummary.onlineServicesEnrolment)
      when(mockOtherServicesFactory.createOnlineServicesDeEnrolment(Matchers.any())).thenReturn(expectedOtherServicesSummary.onlineServicesDeEnrolment)
      when(mockOtherServicesFactory.createBusinessTaxesRegistration(Matchers.any())(org.mockito.Matchers.eq(user))).thenReturn(expectedOtherServicesSummary.businessTaxesRegistration)

      val otherServicesEnrolment = OnlineServicesEnrolment(RenderableLinkMessage(LinkMessage("otherServicesEnrolmentURL", "here", sso = false)))

      val businessTaxesRegistration = BusinessTaxesRegistration(
        Some(RenderableLinkMessage(LinkMessage("http://www.hmrc.gov.uk/online/nex.htm#2", "Register for SA, CT, employers PAYE or VAT", sso = false))),
        RenderableLinkMessage(LinkMessage("http://www.hmrc.gov.uk/online/nex.htm#2", "HMRC website", sso = false))
      )


      val result = Future.successful(controllerUnderTest.otherServicesPage(user, request))

      status(result) shouldBe 200

      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("manageYourTaxesSection") should not be null
      document.getElementById("noOtherServices") should not be null
      document.getElementById("availableServices") shouldBe null

    }

    "render the Other Services template with the generic registration message if the registration link is not available" in new WithApplication(FakeApplication()) with PortalUrlBuilderMock {
      val mockOtherServicesFactory = mock[OtherServicesFactory]
      val controllerUnderTest = new OtherServicesController(mockOtherServicesFactory, null)(null)

      val saRoot = Some(SaRoot(SaUtr("sa-utr"), Map.empty[String, String]))
      val user = User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(sa = saRoot), decryptedToken = None)
      val request = FakeRequest()

      val expectedOtherServicesSummary = OtherServicesSummary(
        Some(ManageYourTaxes(Seq.empty)),
        OnlineServicesEnrolment(RenderableLinkMessage(LinkMessage("otherServicesEnrolmentURL", "here", sso = true))),
        OnlineServicesEnrolment(RenderableLinkMessage(LinkMessage("otherServicesDeEnrolmentURL", "here", sso = true))),
        BusinessTaxesRegistration(
          None,
          RenderableLinkMessage(LinkMessage("http://www.hmrc.gov.uk/online/nex.htm#2", "HMRC website", sso = true)))
      )

      when(mockOtherServicesFactory.createManageYourTaxes(Matchers.any())(Matchers.eq(user), Matchers.any())).thenReturn(expectedOtherServicesSummary.manageYourTaxes)
      when(mockOtherServicesFactory.createOnlineServicesEnrolment(Matchers.any())).thenReturn(expectedOtherServicesSummary.onlineServicesEnrolment)
      when(mockOtherServicesFactory.createOnlineServicesDeEnrolment(Matchers.any())).thenReturn(expectedOtherServicesSummary.onlineServicesDeEnrolment)
      when(mockOtherServicesFactory.createBusinessTaxesRegistration(Matchers.any())(org.mockito.Matchers.eq(user))).thenReturn(expectedOtherServicesSummary.businessTaxesRegistration)

      val otherServicesEnrolment = OnlineServicesEnrolment(RenderableLinkMessage(LinkMessage("otherServicesEnrolmentURL", "here", sso = true)))

      val businessTaxesRegistration = BusinessTaxesRegistration(
        Some(RenderableLinkMessage(LinkMessage("http://www.hmrc.gov.uk/online/nex.htm#2", "Register for SA, CT, employers PAYE or VAT", sso = true))),
        RenderableLinkMessage(LinkMessage("http://www.hmrc.gov.uk/online/nex.htm#2", "HMRC website", sso = true))
      )


      val result = Future.successful(controllerUnderTest.otherServicesPage(user, request))

      status(result) shouldBe 200

      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("registrationGenericMessage") should not be null
      document.getElementById("businessTaxesRegistrationLink") shouldBe null

    }
  }
}
