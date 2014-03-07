package controllers.bt.businesstax

import uk.gov.hmrc.common.BaseSpec
import org.mockito.Mockito._
import play.api.test.Helpers._
import controllers.bt.accountsummary._
import org.mockito.Matchers
import play.api.test.{FakeRequest, WithApplication}
import controllers.bt.testframework.mocks.PortalUrlBuilderMock
import org.scalatest.mock.MockitoSugar
import controllers.bt.{routes => businessTaxRoutes, BusinessTaxController}
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import uk.gov.hmrc.domain._
import uk.gov.hmrc.common.microservice.ct.domain.CtRoot
import uk.gov.hmrc.common.microservice.epaye.domain.{EpayeLinks, EpayeRoot}
import uk.gov.hmrc.common.microservice.vat.domain.VatRoot
import scala.concurrent._
import ExecutionContext.Implicits.global
import org.jsoup.Jsoup
import scala.Some
import controllers.bt.accountsummary.AccountSummaries
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import controllers.bt.accountsummary.AccountSummary
import play.api.test.FakeApplication
import controllers.bt.accountsummary.Msg
import controllers.common.{routes => commonRoutes, FrontEndRedirect}
import uk.gov.hmrc.common.microservice.preferences.{SaEmailPreference, SaPreference, PreferencesConnector}
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.microservice.auth.domain.Authority

class BusinessTaxControllerSpec extends BaseSpec with MockitoSugar {

  import controllers.domain.AuthorityUtils._
  import Matchers.{any, eq => is}

  val utr = SaUtr("sa-utr")
  val saRegime = Some(SaRoot(utr, Map.empty[String, String]))
  val ctRegime = Some(CtRoot(CtUtr("ct-utr"), Map.empty[String, String]))
  val vatRegime = Some(VatRoot(Vrn("vrn"), Map.empty[String, String]))
  val epayeRegime = Some(EpayeRoot(EmpRef.fromIdentifiers("emp/ref"), EpayeLinks(Some("link"))))

  val saAccountSummary = AccountSummary("sa.regimeName", "", List(), Seq.empty, SummaryStatus.success)

  def createUser(saRegime: Option[SaRoot] = None, ctRegime: Option[CtRoot] = None, vatRegime: Option[VatRoot] = None, epayeRegime: Option[EpayeRoot] = None) = {
    User(userId = "userId", userAuthority = allBizTaxAuthority("userId", "sa-utr", "ct-utr", "vrn", "emp/ref"), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(sa = saRegime, ct = ctRegime, vat = vatRegime, epaye = epayeRegime), decryptedToken = None)
  }

  val request = FakeRequest()
  implicit val headerCarrier = HeaderCarrier(request)

  "Calling home with a valid logged in business user when not accessed directly after login" should {

    "always render the navigation links to home, other services and log out" in new BusinessTaxControllerTestSetup {

      val user = createUser(saRegime = saRegime)

      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(user), any())).thenReturn(Future(AccountSummaries(Seq.empty)))

      val result = Future.successful(controllerUnderTest.renderHomePage(user, request))

      status(result) shouldBe 200

      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("homeNavHref").attr("href") shouldBe FrontEndRedirect.businessTaxHome
      document.getElementById("otherServicesNavHref").attr("href") shouldBe "/account" + businessTaxRoutes.OtherServicesController.otherServices().url
      document.getElementById("logOutNavHref").attr("href") shouldBe commonRoutes.LoginController.logout().url

      verifyZeroInteractions(mockPreferencesConnector)
    }

    "render the sa widget" in new BusinessTaxControllerTestSetup {

      val user = createUser(saRegime = saRegime)

      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(user), any())).thenReturn(Future(AccountSummaries(List(saAccountSummary))))

      val homepage = Future.successful(controllerUnderTest.renderHomePage(user, request))

      status(homepage) shouldBe 200

      val document = Jsoup.parse(contentAsString(homepage))
      document.getElementById("sa.regimeName") should not be null
      document.getElementById("ct.regimeName") shouldBe null
      document.getElementById("vat.regimeName") shouldBe null
      document.getElementById("epaye.regimeName") shouldBe null

      verifyZeroInteractions(mockPreferencesConnector)

    }

    "render the epaye widget" in new BusinessTaxControllerTestSetup {

      val accountSummary = AccountSummary("epaye.regimeName", "", List(), Seq.empty, SummaryStatus.success)
      val user = createUser(epayeRegime = epayeRegime)

      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(user), any())).thenReturn(Future(AccountSummaries(List(accountSummary))))

      val homepage = Future.successful(controllerUnderTest.renderHomePage(user, request))

      status(homepage) shouldBe 200

      val document = Jsoup.parse(contentAsString(homepage))
      document.getElementById("epaye.regimeName") should not be null
      document.getElementById("vat.regimeName") shouldBe null
      document.getElementById("sa.regimeName") shouldBe null
      document.getElementById("ct.regimeName") shouldBe null

      verifyZeroInteractions(mockPreferencesConnector)
    }

    "render the ct widget" in new BusinessTaxControllerTestSetup {
      val accountSummary = AccountSummary("ct.regimeName", "", List(), Seq.empty, SummaryStatus.success)
      val user = createUser(ctRegime = ctRegime)

      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(user), any())).thenReturn(Future(AccountSummaries(List(accountSummary))))

      val homepage = Future.successful(controllerUnderTest.renderHomePage(user, request))

      status(homepage) shouldBe 200

      val document = Jsoup.parse(contentAsString(homepage))
      document.getElementById("ct.regimeName") should not be null
      document.getElementById("vat.regimeName") shouldBe null
      document.getElementById("sa.regimeName") shouldBe null
      document.getElementById("epaye.regimeName") shouldBe null

      verifyZeroInteractions(mockPreferencesConnector)
    }

    "render the vat widget" in new BusinessTaxControllerTestSetup {
      val accountSummary = AccountSummary("vat.regimeName", "", List(Msg(VatMessageKeys.vatRegimeNameMessage)), Seq.empty, SummaryStatus.success)
      val user = createUser(vatRegime = vatRegime)

      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(user), any())).thenReturn(Future(AccountSummaries(List(accountSummary))))

      val homepage = Future.successful(controllerUnderTest.renderHomePage(user, request))

      status(homepage) shouldBe 200

      val document = Jsoup.parse(contentAsString(homepage))
      document.getElementById("vat.regimeName") should not be null
      document.getElementById("ct.regimeName") shouldBe null
      document.getElementById("sa.regimeName") shouldBe null
      document.getElementById("epaye.regimeName") shouldBe null

      verifyZeroInteractions(mockPreferencesConnector)
    }

    "render all the widgets in the right order" in new BusinessTaxControllerTestSetup {
      val ctAccountSummary = AccountSummary("ct.regimeName", "", List(), Seq.empty, SummaryStatus.success)
      val vatAccountSummary = AccountSummary("vat.regimeName", "", List(Msg(VatMessageKeys.vatRegimeNameMessage)), Seq.empty, SummaryStatus.success)
      val epayeAcountSummary = AccountSummary("epaye.regimeName", "", List(), Seq.empty, SummaryStatus.success)
      val user = createUser(saRegime, ctRegime, vatRegime, epayeRegime)

      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(user), any())).thenReturn(Future(AccountSummaries(List(saAccountSummary, ctAccountSummary, vatAccountSummary, epayeAcountSummary))))

      val homepage = Future.successful(controllerUnderTest.renderHomePage(user, request))

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

      val accountSummary = AccountSummary("epaye.regimeName", "", List(), Seq.empty, SummaryStatus.success)
      val user = createUser(epayeRegime = epayeRegime)

      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(user), any())).thenReturn(Future(AccountSummaries(List(accountSummary))))

      val homepage = Future.successful(controllerUnderTest.renderHomePage(user, request))

      status(homepage) shouldBe 200

      val document = Jsoup.parse(contentAsString(homepage))

      document.select("h1").text should include("Your tax account")

      verifyZeroInteractions(mockPreferencesConnector)
    }

    "display business tax homepage for SA user with existing print preferences" in new BusinessTaxControllerTestSetup {

      val user = createUser(saRegime)

      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(user), any())).thenReturn(Future(AccountSummaries(List(saAccountSummary))))

      val preference = SaPreference(true, Some(SaEmailPreference("bob@somewhere.stuff", SaEmailPreference.Status.verified)))

      when(mockPreferencesConnector.getPreferences(is(utr))(any())).thenReturn(Some(preference))

      val homepage = Future.successful(controllerUnderTest.businessTaxHomepage(user, request))

      status(homepage) shouldBe 200

      val document = Jsoup.parse(contentAsString(homepage))
      document.select("h1").text should include("Your tax account")
    }

    "render all the widgets even if the services are down" in new BusinessTaxControllerTestSetup {
      val user = createUser(None, None, None, None)

      val ctAccountSummary = AccountSummary("ct.regimeName", "", List(), Seq.empty, SummaryStatus.success)
      val vatAccountSummary = AccountSummary("vat.regimeName", "", List(Msg(VatMessageKeys.vatRegimeNameMessage)), Seq.empty, SummaryStatus.success)
      val epayeAcountSummary = AccountSummary("epaye.regimeName", "", List(), Seq.empty, SummaryStatus.success)

      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(user), any())).thenReturn(Future(AccountSummaries(List(saAccountSummary, ctAccountSummary, vatAccountSummary, epayeAcountSummary))))


      val preference = SaPreference(true, Some(SaEmailPreference("bob@somewhere.stuff", SaEmailPreference.Status.verified)))

      when(mockPreferencesConnector.getPreferences(is(utr))(any())).thenReturn(Some(preference))

      val homepage = Future.successful(controllerUnderTest.businessTaxHomepage(user, request))

      status(homepage) shouldBe 200

      val document = Jsoup.parse(contentAsString(homepage))
      document.select("h1").text should include("Your tax account")

      val elements = document.getElementsByClass("account-summary")
      elements.size shouldBe 4
      elements.get(0).attr("id") shouldBe "sa.regimeName"
      elements.get(1).attr("id") shouldBe "ct.regimeName"
      elements.get(2).attr("id") shouldBe "vat.regimeName"
      elements.get(3).attr("id") shouldBe "epaye.regimeName"
    }

    "redirect to SA print preferences capture page for SA user without existing print preferences" in new BusinessTaxControllerTestSetup {

      val user = createUser(saRegime)

      when(mockAccountSummariesFactory.create(anyOfType[String => String])(Matchers.eq(user), any())).thenReturn(Future(AccountSummaries(List(saAccountSummary))))

      when(mockPreferencesConnector.getPreferences(is(utr))(any())).thenReturn(None)

      val response = Future.successful(controllerUnderTest.businessTaxHomepage(user, request))

      status(response) shouldBe 303
      header("Location", response).get should include("/account-details/sa/login-opt-in-email-reminders")

      verify(mockPreferencesConnector).getPreferences(is(utr))(any())
    }
  }

}

abstract class BusinessTaxControllerTestSetup extends WithApplication(FakeApplication()) with PortalUrlBuilderMock {

  val mockAccountSummariesFactory = mock[AccountSummariesFactory]

  val mockPreferencesConnector = mock[PreferencesConnector]

  val controllerUnderTest = new BusinessTaxController(mockAccountSummariesFactory, mockPreferencesConnector, null)(null) with MockedPortalUrlBuilder {
    override def regimeRoots(authority: Authority)(implicit hc: HeaderCarrier) = Future.successful(RegimeRoots())
  }

  when(mockPortalUrlBuilder.buildPortalUrl("otherServices")).thenReturn("otherServicesUrl")
  when(mockPortalUrlBuilder.buildPortalUrl("otherServicesEnrolment")).thenReturn("otherServicesEnrolmentUrl")
  when(mockPortalUrlBuilder.buildPortalUrl("servicesDeEnrolment")).thenReturn("servicesDeEnrolmentUrl")
}