package controllers.bt.businesstax

import uk.gov.hmrc.common.BaseSpec
import org.mockito.Mockito._
import play.api.test.Helpers._
import controllers.bt.accountsummary._
import org.mockito.Matchers
import play.api.test.{FakeRequest, WithApplication}
import controllers.bt.testframework.mocks.PortalUrlBuilderMock
import org.scalatest.mock.MockitoSugar
import controllers.bt.BusinessTaxController
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import uk.gov.hmrc.domain._
import uk.gov.hmrc.common.microservice.ct.domain.CtRoot
import uk.gov.hmrc.common.microservice.epaye.domain.{EpayeLinks, EpayeRoot}
import uk.gov.hmrc.common.microservice.vat.domain.VatRoot
import scala.concurrent.Future
import org.jsoup.Jsoup
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import scala.Some
import controllers.bt.accountsummary.AccountSummaries
import uk.gov.hmrc.common.microservice.auth.domain.Regimes
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import controllers.bt.accountsummary.AccountSummary
import play.api.test.FakeApplication
import controllers.bt.accountsummary.Msg
import controllers.bt.{routes => businessTaxRoutes}
import controllers.common.{routes => commonRoutes}

class BusinessTaxControllerSpec extends BaseSpec with MockitoSugar {

  "Calling home with a valid logged in business user" should {

    "always render the navigation links to home, other services and log out" in new WithApplication(FakeApplication()) with PortalUrlBuilderMock {

      val mockAccountSummariesFactory = mock[AccountSummariesFactory]
      val controllerUnderTest = new BusinessTaxController(mockAccountSummariesFactory) with MockedPortalUrlBuilder

      val saRoot = Some(SaRoot(SaUtr("sa-utr"), Map.empty[String, String]))
      val user = User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(sa = saRoot), decryptedToken = None)
      val request = FakeRequest()

      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(user))).thenReturn(AccountSummaries(Seq.empty))
      when(mockPortalUrlBuilder.buildPortalUrl("otherServices")).thenReturn("otherServicesUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("otherServicesEnrolment")).thenReturn("otherServicesEnrolmentUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("servicesDeEnrolment")).thenReturn("servicesDeEnrolmentUrl")

      val result = Future.successful(controllerUnderTest.businessTaxHomepage(user, request))

      status(result) shouldBe 200

      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("homeNavHref").attr("href") shouldBe commonRoutes.HomeController.home().url
      document.getElementById("otherServicesNavHref").attr("href") shouldBe "/business-tax" + businessTaxRoutes.OtherServicesController.otherServices().url
      document.getElementById("logOutNavHref").attr("href") shouldBe commonRoutes.LoginController.logout().url
    }

    "always render the sso links to enrol, de-enrol and manage services" in new WithApplication(FakeApplication()) with PortalUrlBuilderMock {

      val mockAccountSummariesFactory = mock[AccountSummariesFactory]
      val controllerUnderTest = new BusinessTaxController(mockAccountSummariesFactory) with MockedPortalUrlBuilder

      val saRoot = Some(SaRoot(SaUtr("sa-utr"), Map.empty[String, String]))
      val user = User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(sa = saRoot), decryptedToken = None)
      val request = FakeRequest()

      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(user))).thenReturn(AccountSummaries(Seq.empty))
      when(mockPortalUrlBuilder.buildPortalUrl("otherServices")).thenReturn("otherServicesUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("otherServicesEnrolment")).thenReturn("otherServicesEnrolmentUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("servicesDeEnrolment")).thenReturn("servicesDeEnrolmentUrl")

      val result = Future.successful(controllerUnderTest.businessTaxHomepage(user, request))

      status(result) shouldBe 200

      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("otherServicesHref").attr("href") shouldBe "otherServicesUrl"
      document.getElementById("enrolServiceHref1").attr("href") shouldBe "otherServicesEnrolmentUrl"
      document.getElementById("enrolServiceHref2").attr("href") shouldBe "otherServicesEnrolmentUrl"
      document.getElementById("removeServiceHref").attr("href") shouldBe "servicesDeEnrolmentUrl"
    }

    "render the sa widget" in new WithApplication(FakeApplication()) with PortalUrlBuilderMock {
      val saAccountSummary = AccountSummary("sa.regimeName", List(Msg(SaMessageKeys.saUtrMessage)), Seq.empty, SummaryStatus.success)

      val saRoot = Some(SaRoot(SaUtr("sa-utr"), Map.empty[String, String]))
      val user = User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(sa = saRoot), decryptedToken = None)

      val mockAccountSummariesFactory = mock[AccountSummariesFactory]
      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(user))).thenReturn(AccountSummaries(List(saAccountSummary)))


      when(mockPortalUrlBuilder.buildPortalUrl("otherServices")).thenReturn("otherServicesUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("otherServicesEnrolment")).thenReturn("otherServicesEnrolmentUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("servicesDeEnrolment")).thenReturn("servicesDeEnrolmentUrl")

      val controllerUnderTest = new BusinessTaxController(mockAccountSummariesFactory) with MockedPortalUrlBuilder

      val homepage = Future.successful(controllerUnderTest.businessTaxHomepage(user, FakeRequest()))

      status(homepage) shouldBe 200

      val document = Jsoup.parse(contentAsString(homepage))
      document.getElementById("sa.regimeName") should not be null
      document.getElementById("ct.regimeName") shouldBe null
      document.getElementById("vat.regimeName") shouldBe null
      document.getElementById("epaye.regimeName") shouldBe null

    }

    "render the epaye widget" in new WithApplication(FakeApplication()) with PortalUrlBuilderMock {
      val accountSummary = AccountSummary("epaye.regimeName", List(Msg(EpayeMessageKeys.epayeEmpRefMessage)), Seq.empty, SummaryStatus.success)

      val regime = Some(EpayeRoot(EmpRef("some emp/ref"), EpayeLinks(Some("link"))))
      val user = User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()),
        nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(epaye = regime), decryptedToken = None)

      val mockAccountSummariesFactory = mock[AccountSummariesFactory]
      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(user))).thenReturn(AccountSummaries(List(accountSummary)))


      when(mockPortalUrlBuilder.buildPortalUrl("otherServices")).thenReturn("otherServicesUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("otherServicesEnrolment")).thenReturn("otherServicesEnrolmentUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("servicesDeEnrolment")).thenReturn("servicesDeEnrolmentUrl")

      val controllerUnderTest = new BusinessTaxController(mockAccountSummariesFactory) with MockedPortalUrlBuilder

      val homepage = Future.successful(controllerUnderTest.businessTaxHomepage(user, FakeRequest()))

      status(homepage) shouldBe 200

      val document = Jsoup.parse(contentAsString(homepage))
      document.getElementById("epaye.regimeName") should not be null
      document.getElementById("vat.regimeName") shouldBe null
      document.getElementById("sa.regimeName") shouldBe null
      document.getElementById("ct.regimeName") shouldBe null
    }

    "render the ct widget" in new WithApplication(FakeApplication()) with PortalUrlBuilderMock {
      val accountSummary = AccountSummary("ct.regimeName", List(Msg(CtMessageKeys.ctUtrMessage)), Seq.empty, SummaryStatus.success)

      val regime = Some(CtRoot(CtUtr("some ct utr"), Map.empty[String, String]))
      val user = User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()),
        nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(ct = regime), decryptedToken = None)

      val mockAccountSummariesFactory = mock[AccountSummariesFactory]
      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(user))).thenReturn(AccountSummaries(List(accountSummary)))


      when(mockPortalUrlBuilder.buildPortalUrl("otherServices")).thenReturn("otherServicesUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("otherServicesEnrolment")).thenReturn("otherServicesEnrolmentUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("servicesDeEnrolment")).thenReturn("servicesDeEnrolmentUrl")

      val controllerUnderTest = new BusinessTaxController(mockAccountSummariesFactory) with MockedPortalUrlBuilder

      val homepage = Future.successful(controllerUnderTest.businessTaxHomepage(user, FakeRequest()))

      status(homepage) shouldBe 200

      val document = Jsoup.parse(contentAsString(homepage))
      document.getElementById("ct.regimeName") should not be null
      document.getElementById("vat.regimeName") shouldBe null
      document.getElementById("sa.regimeName") shouldBe null
      document.getElementById("epaye.regimeName") shouldBe null
    }

    "render the vat widget" in new WithApplication(FakeApplication()) with PortalUrlBuilderMock {
      val accountSummary = AccountSummary("vat.regimeName", List(Msg(VatMessageKeys.vatRegimeNameMessage)), Seq.empty, SummaryStatus.success)

      val regime = Some(VatRoot(Vrn("some vrn"), Map.empty[String, String]))
      val user = User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(vat = regime), decryptedToken = None)

      val mockAccountSummariesFactory = mock[AccountSummariesFactory]
      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(user))).thenReturn(AccountSummaries(List(accountSummary)))


      when(mockPortalUrlBuilder.buildPortalUrl("otherServices")).thenReturn("otherServicesUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("otherServicesEnrolment")).thenReturn("otherServicesEnrolmentUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("servicesDeEnrolment")).thenReturn("servicesDeEnrolmentUrl")

      val controllerUnderTest = new BusinessTaxController(mockAccountSummariesFactory) with MockedPortalUrlBuilder

      val homepage = Future.successful(controllerUnderTest.businessTaxHomepage(user, FakeRequest()))

      status(homepage) shouldBe 200

      val document = Jsoup.parse(contentAsString(homepage))
      document.getElementById("vat.regimeName") should not be null
      document.getElementById("ct.regimeName") shouldBe null
      document.getElementById("sa.regimeName") shouldBe null
      document.getElementById("epaye.regimeName") shouldBe null
    }

    "render all the widgets in the right order" in new WithApplication(FakeApplication()) with PortalUrlBuilderMock  {
      val saAccountSummary = AccountSummary("sa.regimeName", List(Msg(SaMessageKeys.saUtrMessage)), Seq.empty, SummaryStatus.success)
      val ctAccountSummary = AccountSummary("ct.regimeName", List(Msg(CtMessageKeys.ctUtrMessage)), Seq.empty, SummaryStatus.success)
      val vatAccountSummary = AccountSummary("vat.regimeName", List(Msg(VatMessageKeys.vatRegimeNameMessage)), Seq.empty, SummaryStatus.success)
      val epayeAcountSummary = AccountSummary("epaye.regimeName", List(Msg(EpayeMessageKeys.epayeEmpRefMessage)), Seq.empty, SummaryStatus.success)

      val saRegime = Some(SaRoot(SaUtr("sa-utr"), Map.empty[String, String]))
      val ctRegime = Some(CtRoot(CtUtr("some ct utr"), Map.empty[String, String]))
      val vatRegime = Some(VatRoot(Vrn("some vrn"), Map.empty[String, String]))
      val epayeRegime = Some(EpayeRoot(EmpRef("some emp/ref"), EpayeLinks(Some("link"))))

      val user = User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(None, saRegime, vatRegime, epayeRegime, ctRegime), decryptedToken = None)

      val mockAccountSummariesFactory = mock[AccountSummariesFactory]
      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(user))).thenReturn(AccountSummaries(List(saAccountSummary, ctAccountSummary, vatAccountSummary,epayeAcountSummary)))

      when(mockPortalUrlBuilder.buildPortalUrl("otherServices")).thenReturn("otherServicesUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("otherServicesEnrolment")).thenReturn("otherServicesEnrolmentUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("servicesDeEnrolment")).thenReturn("servicesDeEnrolmentUrl")

      val controllerUnderTest = new BusinessTaxController(mockAccountSummariesFactory) with MockedPortalUrlBuilder

      val homepage = Future.successful(controllerUnderTest.businessTaxHomepage(user, FakeRequest()))

      status(homepage) shouldBe 200

      val document = Jsoup.parse(contentAsString(homepage))

      val elements = document.getElementsByClass("account-summary")
      elements.size shouldBe 4
      elements.get(0).attr("id") shouldBe "sa.regimeName"
      elements.get(1).attr("id") shouldBe "ct.regimeName"
      elements.get(2).attr("id") shouldBe "vat.regimeName"
      elements.get(3).attr("id") shouldBe "epaye.regimeName"

    }

  }
}
