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
import scala.concurrent._
import ExecutionContext.Implicits.global
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
<<<<<<< HEAD
import uk.gov.hmrc.common.microservice.preferences.{SaPreference, PreferencesConnector}
=======
import controllers.common.actions.HeaderCarrier
>>>>>>> Doug [HMTB-2047] I have introduced the HeaderCarrier class, which will replace the MDC code in the long-term. MDC uses a thread-local variable to hold the header information from the incoming request for retrieval by the Connector when making WS requests to the microservices. This is a problem when we introduce Futures as the thread making the WS call is no longer the one that received the request, so we need another way to carry that information through. The controller can build a HeaderCarrier from a Request and it will be passed down the chain of calls through the builders as an implicit param. I have also created AsyncController, which has all the same methods as Controller, but returning Futures

class BusinessTaxControllerSpec extends BaseSpec with MockitoSugar {
  
  "Calling home with a valid logged in business user when not accessed directly after login" should {

    "always render the navigation links to home, other services and log out" in new BusinessTaxControllerTestSetup {

      val saRoot = Some(SaRoot(SaUtr("sa-utr"), Map.empty[String, String]))
      val user = User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(sa = saRoot), decryptedToken = None)
      val request = FakeRequest()

      implicit val headerCarrier = HeaderCarrier(request)
      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(user), Matchers.eq(headerCarrier))).thenReturn(Future(AccountSummaries(Seq.empty)))
      when(mockPortalUrlBuilder.buildPortalUrl("otherServices")).thenReturn("otherServicesUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("otherServicesEnrolment")).thenReturn("otherServicesEnrolmentUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("servicesDeEnrolment")).thenReturn("servicesDeEnrolmentUrl")

      val result = Future.successful(controllerUnderTest.businessTaxHomepage(Some("false"))(user, request))

      status(result) shouldBe 200

      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("homeNavHref").attr("href") shouldBe commonRoutes.HomeController.home().url
      document.getElementById("otherServicesNavHref").attr("href") shouldBe "/business-tax" + businessTaxRoutes.OtherServicesController.otherServices().url
      document.getElementById("logOutNavHref").attr("href") shouldBe commonRoutes.LoginController.logout().url

      verifyZeroInteractions(mockPreferencesConnector)
    }

    "always render the sso links to enrol, de-enrol and manage services" in new BusinessTaxControllerTestSetup {

      val saRoot = Some(SaRoot(SaUtr("sa-utr"), Map.empty[String, String]))
      val user = User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(sa = saRoot), decryptedToken = None)
      val request = FakeRequest()

      implicit val headerCarrier = HeaderCarrier()
      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(user), Matchers.eq(headerCarrier))).thenReturn(Future(AccountSummaries(Seq.empty)))
      when(mockPortalUrlBuilder.buildPortalUrl("otherServicesEnrolment")).thenReturn("otherServicesEnrolmentUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("servicesDeEnrolment")).thenReturn("servicesDeEnrolmentUrl")

      val result = Future.successful(controllerUnderTest.businessTaxHomepage(Some("notTrue"))(user, request))

      status(result) shouldBe 200

      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("enrolServiceHref1").attr("href") shouldBe "otherServicesEnrolmentUrl"
      document.getElementById("enrolServiceHref2").attr("href") shouldBe "otherServicesEnrolmentUrl"
      document.getElementById("removeServiceHref").attr("href") shouldBe "servicesDeEnrolmentUrl"

      verifyZeroInteractions(mockPreferencesConnector)
    }

    "render the sa widget" in new BusinessTaxControllerTestSetup {
      val saAccountSummary = AccountSummary("sa.regimeName", List(Msg(SaMessageKeys.saUtrMessage)), Seq.empty, SummaryStatus.success)

      val saRoot = Some(SaRoot(SaUtr("sa-utr"), Map.empty[String, String]))
      val user = User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(sa = saRoot), decryptedToken = None)

<<<<<<< HEAD
      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(user))).thenReturn(Future(AccountSummaries(List(saAccountSummary))))

=======
      implicit val headerCarrier = HeaderCarrier()
      val mockAccountSummariesFactory = mock[AccountSummariesFactory]
      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(user), Matchers.eq(headerCarrier))).thenReturn(Future(AccountSummaries(List(saAccountSummary))))
>>>>>>> Doug [HMTB-2047] I have introduced the HeaderCarrier class, which will replace the MDC code in the long-term. MDC uses a thread-local variable to hold the header information from the incoming request for retrieval by the Connector when making WS requests to the microservices. This is a problem when we introduce Futures as the thread making the WS call is no longer the one that received the request, so we need another way to carry that information through. The controller can build a HeaderCarrier from a Request and it will be passed down the chain of calls through the builders as an implicit param. I have also created AsyncController, which has all the same methods as Controller, but returning Futures

      when(mockPortalUrlBuilder.buildPortalUrl("otherServices")).thenReturn("otherServicesUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("otherServicesEnrolment")).thenReturn("otherServicesEnrolmentUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("servicesDeEnrolment")).thenReturn("servicesDeEnrolmentUrl")

      val homepage = Future.successful(controllerUnderTest.businessTaxHomepage(None)(user, FakeRequest()))

      status(homepage) shouldBe 200

      val document = Jsoup.parse(contentAsString(homepage))
      document.getElementById("sa.regimeName") should not be null
      document.getElementById("ct.regimeName") shouldBe null
      document.getElementById("vat.regimeName") shouldBe null
      document.getElementById("epaye.regimeName") shouldBe null

      verifyZeroInteractions(mockPreferencesConnector)

    }

    "render the epaye widget" in new BusinessTaxControllerTestSetup {
      val accountSummary = AccountSummary("epaye.regimeName", List(Msg(EpayeMessageKeys.epayeEmpRefMessage)), Seq.empty, SummaryStatus.success)

      val regime = Some(EpayeRoot(EmpRef("some emp/ref"), EpayeLinks(Some("link"))))
      val user = User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()),
        nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(epaye = regime), decryptedToken = None)

<<<<<<< HEAD
      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(user))).thenReturn(Future(AccountSummaries(List(accountSummary))))
=======
      implicit val headerCarrier = HeaderCarrier()
      val mockAccountSummariesFactory = mock[AccountSummariesFactory]
      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(user), Matchers.eq(headerCarrier))).thenReturn(Future(AccountSummaries(List(accountSummary))))
>>>>>>> Doug [HMTB-2047] I have introduced the HeaderCarrier class, which will replace the MDC code in the long-term. MDC uses a thread-local variable to hold the header information from the incoming request for retrieval by the Connector when making WS requests to the microservices. This is a problem when we introduce Futures as the thread making the WS call is no longer the one that received the request, so we need another way to carry that information through. The controller can build a HeaderCarrier from a Request and it will be passed down the chain of calls through the builders as an implicit param. I have also created AsyncController, which has all the same methods as Controller, but returning Futures

      when(mockPortalUrlBuilder.buildPortalUrl("otherServices")).thenReturn("otherServicesUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("otherServicesEnrolment")).thenReturn("otherServicesEnrolmentUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("servicesDeEnrolment")).thenReturn("servicesDeEnrolmentUrl")

      val homepage = Future.successful(controllerUnderTest.businessTaxHomepage(Some("false"))(user, FakeRequest()))

      status(homepage) shouldBe 200

      val document = Jsoup.parse(contentAsString(homepage))
      document.getElementById("epaye.regimeName") should not be null
      document.getElementById("vat.regimeName") shouldBe null
      document.getElementById("sa.regimeName") shouldBe null
      document.getElementById("ct.regimeName") shouldBe null

      verifyZeroInteractions(mockPreferencesConnector)
    }

    "render the ct widget" in new BusinessTaxControllerTestSetup {
      val accountSummary = AccountSummary("ct.regimeName", List(Msg(CtMessageKeys.ctUtrMessage)), Seq.empty, SummaryStatus.success)

      val regime = Some(CtRoot(CtUtr("some ct utr"), Map.empty[String, String]))
      val user = User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()),
        nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(ct = regime), decryptedToken = None)

<<<<<<< HEAD
      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(user))).thenReturn(Future(AccountSummaries(List(accountSummary))))
=======
      implicit val headerCarrier = HeaderCarrier()
      val mockAccountSummariesFactory = mock[AccountSummariesFactory]
      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(user), Matchers.eq(headerCarrier))).thenReturn(Future(AccountSummaries(List(accountSummary))))
>>>>>>> Doug [HMTB-2047] I have introduced the HeaderCarrier class, which will replace the MDC code in the long-term. MDC uses a thread-local variable to hold the header information from the incoming request for retrieval by the Connector when making WS requests to the microservices. This is a problem when we introduce Futures as the thread making the WS call is no longer the one that received the request, so we need another way to carry that information through. The controller can build a HeaderCarrier from a Request and it will be passed down the chain of calls through the builders as an implicit param. I have also created AsyncController, which has all the same methods as Controller, but returning Futures


      when(mockPortalUrlBuilder.buildPortalUrl("otherServices")).thenReturn("otherServicesUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("otherServicesEnrolment")).thenReturn("otherServicesEnrolmentUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("servicesDeEnrolment")).thenReturn("servicesDeEnrolmentUrl")

      val homepage = Future.successful(controllerUnderTest.businessTaxHomepage(None)(user, FakeRequest()))

      status(homepage) shouldBe 200

      val document = Jsoup.parse(contentAsString(homepage))
      document.getElementById("ct.regimeName") should not be null
      document.getElementById("vat.regimeName") shouldBe null
      document.getElementById("sa.regimeName") shouldBe null
      document.getElementById("epaye.regimeName") shouldBe null

      verifyZeroInteractions(mockPreferencesConnector)
    }

    "render the vat widget" in new BusinessTaxControllerTestSetup {
      val accountSummary = AccountSummary("vat.regimeName", List(Msg(VatMessageKeys.vatRegimeNameMessage)), Seq.empty, SummaryStatus.success)

      val regime = Some(VatRoot(Vrn("some vrn"), Map.empty[String, String]))
      val user = User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(vat = regime), decryptedToken = None)

<<<<<<< HEAD
      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(user))).thenReturn(Future(AccountSummaries(List(accountSummary))))
=======
      implicit val headerCarrier = HeaderCarrier()
      val mockAccountSummariesFactory = mock[AccountSummariesFactory]
      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(user), Matchers.eq(headerCarrier))).thenReturn(Future(AccountSummaries(List(accountSummary))))
>>>>>>> Doug [HMTB-2047] I have introduced the HeaderCarrier class, which will replace the MDC code in the long-term. MDC uses a thread-local variable to hold the header information from the incoming request for retrieval by the Connector when making WS requests to the microservices. This is a problem when we introduce Futures as the thread making the WS call is no longer the one that received the request, so we need another way to carry that information through. The controller can build a HeaderCarrier from a Request and it will be passed down the chain of calls through the builders as an implicit param. I have also created AsyncController, which has all the same methods as Controller, but returning Futures

      when(mockPortalUrlBuilder.buildPortalUrl("otherServices")).thenReturn("otherServicesUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("otherServicesEnrolment")).thenReturn("otherServicesEnrolmentUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("servicesDeEnrolment")).thenReturn("servicesDeEnrolmentUrl")

      val homepage = Future.successful(controllerUnderTest.businessTaxHomepage(None)(user, FakeRequest()))

      status(homepage) shouldBe 200

      val document = Jsoup.parse(contentAsString(homepage))
      document.getElementById("vat.regimeName") should not be null
      document.getElementById("ct.regimeName") shouldBe null
      document.getElementById("sa.regimeName") shouldBe null
      document.getElementById("epaye.regimeName") shouldBe null

      verifyZeroInteractions(mockPreferencesConnector)
    }

<<<<<<< HEAD
    "render all the widgets in the right order" in new BusinessTaxControllerTestSetup  {
=======
    "render all the widgets in the right order" in new WithApplication(FakeApplication()) with PortalUrlBuilderMock {
>>>>>>> Doug [HMTB-2047] I have introduced the HeaderCarrier class, which will replace the MDC code in the long-term. MDC uses a thread-local variable to hold the header information from the incoming request for retrieval by the Connector when making WS requests to the microservices. This is a problem when we introduce Futures as the thread making the WS call is no longer the one that received the request, so we need another way to carry that information through. The controller can build a HeaderCarrier from a Request and it will be passed down the chain of calls through the builders as an implicit param. I have also created AsyncController, which has all the same methods as Controller, but returning Futures
      val saAccountSummary = AccountSummary("sa.regimeName", List(Msg(SaMessageKeys.saUtrMessage)), Seq.empty, SummaryStatus.success)
      val ctAccountSummary = AccountSummary("ct.regimeName", List(Msg(CtMessageKeys.ctUtrMessage)), Seq.empty, SummaryStatus.success)
      val vatAccountSummary = AccountSummary("vat.regimeName", List(Msg(VatMessageKeys.vatRegimeNameMessage)), Seq.empty, SummaryStatus.success)
      val epayeAcountSummary = AccountSummary("epaye.regimeName", List(Msg(EpayeMessageKeys.epayeEmpRefMessage)), Seq.empty, SummaryStatus.success)

      val saRegime = Some(SaRoot(SaUtr("sa-utr"), Map.empty[String, String]))
      val ctRegime = Some(CtRoot(CtUtr("some ct utr"), Map.empty[String, String]))
      val vatRegime = Some(VatRoot(Vrn("some vrn"), Map.empty[String, String]))
      val epayeRegime = Some(EpayeRoot(EmpRef("some emp/ref"), EpayeLinks(Some("link"))))

      val user = User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(None, saRegime, vatRegime, epayeRegime, ctRegime), decryptedToken = None)

<<<<<<< HEAD
      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(user))).thenReturn(Future(AccountSummaries(List(saAccountSummary, ctAccountSummary, vatAccountSummary,epayeAcountSummary))))
=======
      implicit val headerCarrier = HeaderCarrier()
      val mockAccountSummariesFactory = mock[AccountSummariesFactory]
      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(user), Matchers.eq(headerCarrier))).thenReturn(Future(AccountSummaries(List(saAccountSummary, ctAccountSummary, vatAccountSummary, epayeAcountSummary))))
>>>>>>> Doug [HMTB-2047] I have introduced the HeaderCarrier class, which will replace the MDC code in the long-term. MDC uses a thread-local variable to hold the header information from the incoming request for retrieval by the Connector when making WS requests to the microservices. This is a problem when we introduce Futures as the thread making the WS call is no longer the one that received the request, so we need another way to carry that information through. The controller can build a HeaderCarrier from a Request and it will be passed down the chain of calls through the builders as an implicit param. I have also created AsyncController, which has all the same methods as Controller, but returning Futures

      when(mockPortalUrlBuilder.buildPortalUrl("otherServices")).thenReturn("otherServicesUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("otherServicesEnrolment")).thenReturn("otherServicesEnrolmentUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("servicesDeEnrolment")).thenReturn("servicesDeEnrolmentUrl")

      val homepage = Future.successful(controllerUnderTest.businessTaxHomepage(None)(user, FakeRequest()))

      status(homepage) shouldBe 200

      val document = Jsoup.parse(contentAsString(homepage))

      val elements = document.getElementsByClass("account-summary")
      elements.size shouldBe 4
      elements.get(0).attr("id") shouldBe "sa.regimeName"
      elements.get(1).attr("id") shouldBe "ct.regimeName"
      elements.get(2).attr("id") shouldBe "vat.regimeName"
      elements.get(3).attr("id") shouldBe "epaye.regimeName"

      verifyZeroInteractions(mockPreferencesConnector)
    }

  }

  "Calling home with a valid logged in business user when accessed just after logging in" should {

    "display business tax homepage for user without SA regime" in new BusinessTaxControllerTestSetup {

      val accountSummary = AccountSummary("epaye.regimeName", List(Msg(EpayeMessageKeys.epayeEmpRefMessage)), Seq.empty, SummaryStatus.success)

      val regime = Some(EpayeRoot(EmpRef("some emp/ref"), EpayeLinks(Some("link"))))
      val user = User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()),
        nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(epaye = regime), decryptedToken = None)

      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(user))).thenReturn(Future(AccountSummaries(List(accountSummary))))

      when(mockPortalUrlBuilder.buildPortalUrl("otherServices")).thenReturn("otherServicesUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("otherServicesEnrolment")).thenReturn("otherServicesEnrolmentUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("servicesDeEnrolment")).thenReturn("servicesDeEnrolmentUrl")

      val homepage = Future.successful(controllerUnderTest.businessTaxHomepage(Some("true"))(user, FakeRequest()))

      status(homepage) shouldBe 200

      val document = Jsoup.parse(contentAsString(homepage))

      document.select("h1").text should include("Your tax account")

      verifyZeroInteractions(mockPreferencesConnector)
    }

    "display business tax homepage for SA user with existing print preferences" in new BusinessTaxControllerTestSetup {

      val saAccountSummary = AccountSummary("sa.regimeName", List(Msg(SaMessageKeys.saUtrMessage)), Seq.empty, SummaryStatus.success)

      val utr = SaUtr("sa-utr")
      val saRoot = Some(SaRoot(utr, Map.empty[String, String]))
      val user = User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(sa = saRoot), decryptedToken = None)

      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(user))).thenReturn(Future(AccountSummaries(List(saAccountSummary))))

      when(mockPortalUrlBuilder.buildPortalUrl("otherServices")).thenReturn("otherServicesUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("otherServicesEnrolment")).thenReturn("otherServicesEnrolmentUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("servicesDeEnrolment")).thenReturn("servicesDeEnrolmentUrl")

      val preference = SaPreference(digital = true, Some("bob@somewhere.stuff"))
      when(mockPreferencesConnector.getPreferences(utr)).thenReturn(Some(preference))

      val homepage = Future.successful(controllerUnderTest.businessTaxHomepage(Some("true"))(user, FakeRequest()))

      status(homepage) shouldBe 200

      val document = Jsoup.parse(contentAsString(homepage))
      document.select("h1").text should include("Your tax account")

      verify(mockPreferencesConnector).getPreferences(utr)
    }

    "redirect to SA print preferences capture page for SA user without existing print preferences" in new BusinessTaxControllerTestSetup {

      val saAccountSummary = AccountSummary("sa.regimeName", List(Msg(SaMessageKeys.saUtrMessage)), Seq.empty, SummaryStatus.success)

      val utr = SaUtr("sa-utr")
      val saRoot = Some(SaRoot(utr, Map.empty[String, String]))
      val user = User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(sa = saRoot), decryptedToken = None)

      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(user))).thenReturn(Future(AccountSummaries(List(saAccountSummary))))

      when(mockPortalUrlBuilder.buildPortalUrl("otherServices")).thenReturn("otherServicesUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("otherServicesEnrolment")).thenReturn("otherServicesEnrolmentUrl")
      when(mockPortalUrlBuilder.buildPortalUrl("servicesDeEnrolment")).thenReturn("servicesDeEnrolmentUrl")

      when(mockPreferencesConnector.getPreferences(utr)).thenReturn(None)

      val response = Future.successful(controllerUnderTest.businessTaxHomepage(Some("true"))(user, FakeRequest()))

      status(response) shouldBe 303
      header("Location", response).get should include("/prefs/sa/print")

      verify(mockPreferencesConnector).getPreferences(utr)
    }
  }
}

abstract class BusinessTaxControllerTestSetup extends WithApplication(FakeApplication()) with PortalUrlBuilderMock {

  val mockAccountSummariesFactory = mock[AccountSummariesFactory]

  val mockPreferencesConnector = mock[PreferencesConnector]

  val controllerUnderTest = new BusinessTaxController(mockAccountSummariesFactory, mockPreferencesConnector, null)(null) with MockedPortalUrlBuilder

}
