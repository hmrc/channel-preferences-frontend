package service

import connectors.{AuthConnector, PaperlessPreference, PaperlessService, PreferencesConnector}
import model.HostContext
import org.mockito.Matchers.{any, eq => argEq}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import uk.gov.hmrc.domain.TaxIds.TaxIdWithName
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PaperlessActivateServiceSpec extends UnitSpec with ScalaFutures with OneAppPerSuite {

  "PaperlessActivateService" should {

    implicit val hc = HeaderCarrier()

    "Return the PreferenceFound for default service for the taxId when asked for default service" in new TestCase {
      val service: PaperlessActivateService = createService(saPaperlessPreferenceForDefaultService)(sautr)
      service.paperlessPreference(HostContext("returnUrl", "returnLinkText"), "default").futureValue shouldBe PreferenceFound
    }

    "Return the RedirectToOptIn when asked for taxCredits service and only default service preference is there" in new TestCase {
      val service: PaperlessActivateService = createService(saPaperlessPreferenceForDefaultService)(sautr)
      service.paperlessPreference(HostContext("returnUrl", "returnLinkText"), "taxCredit").futureValue shouldBe
        RedirectToOptInPage("taxCredit", "/paperless/choose?returnUrl=7bHIREJ6fizedcIK4IwOVw%3D%3D&returnLinkText=1wDSitF2l%2F0HsWw3huU5nQ%3D%3D")
    }

    "Return the UserAutoOptIn when there is an saUtr preference and not a Nino one if service is default" in new TestCase {
      val service: PaperlessActivateService = createService(saPaperlessPreferenceForDefaultService)(sautr, nino)
      service.paperlessPreference(HostContext("returnUrl", "returnLinkText"), "default").futureValue shouldBe UserAutoOptIn(nino, "default")
    }

    "Return the PreferenceFound for TaxCredits when there is an Nino preference for TaxCredits and don't auto optIn the user to Nino default" in new TestCase {

      val preferenceMap = Map(
        sautr -> Future.successful(Some(PaperlessPreference(Map("default" -> PaperlessService(true, "genericTerms")), None))),
        nino -> Future.successful(Some(PaperlessPreference(Map("taxCredits" -> PaperlessService(true, "genericTerms")), None)))
      )

      val service: PaperlessActivateService = createService(preferenceMap)(sautr, nino)
      val taxCreditsPreferenceResponse = service.paperlessPreference(HostContext("returnUrl", "returnLinkText"), "taxCredits").futureValue
      taxCreditsPreferenceResponse shouldBe PreferenceFound
    }

    "Return the AutoOptIn for saUtr and Default service when there is an Nino preference for TaxCredits and Default but not for SaUtr " in new TestCase {

      val preferenceMap = Map(
        nino -> Future.successful(
          Some(PaperlessPreference(
            Map(
              "taxCredits" -> PaperlessService(true, "genericTerms"),
              "default" -> PaperlessService(true, "genericTerms")),
            None)
          )))

      val service: PaperlessActivateService = createService(preferenceMap)(sautr, nino)
      val defaultPreferenceResponse = service.paperlessPreference(HostContext("returnUrl", "returnLinkText"), "default").futureValue
      defaultPreferenceResponse shouldBe UserAutoOptIn(sautr, "default")
    }

    "Return Unauthorised if there are no saUtr or Nino tax identifiers associated to the user in the Auth record" in new TestCase {
      private val noTaxIdentifiers: Seq[TaxIdWithName] = Seq()
      val service: PaperlessActivateService = createService(saPaperlessPreferenceForDefaultService)(noTaxIdentifiers: _*)
      service.paperlessPreference(HostContext("returnUrl", "returnLinkText"), "default").futureValue shouldBe UnAuthorised
    }

    "Return RedirectToOptIn page when the user has no preference saved" in new TestCase {
      val service: PaperlessActivateService = createService(Map(sautr -> Future.successful(None)))(sautr)
      service.paperlessPreference(HostContext("returnUrl", "returnLinkText"), "default").futureValue shouldBe
        RedirectToOptInPage("default", "/paperless/choose?returnUrl=7bHIREJ6fizedcIK4IwOVw%3D%3D&returnLinkText=1wDSitF2l%2F0HsWw3huU5nQ%3D%3D")
    }

  }

  trait TestCase extends MockitoSugar {
    val nino: TaxIdWithName = Nino("AB123456A")
    val sautr: TaxIdWithName = SaUtr("123456789")

    val saPaperlessPreferenceForDefaultService =
      Map(
        sautr -> Future.successful(Some(PaperlessPreference(Map("default" -> PaperlessService(true, "genericTerms")), None)))
      )

    val saAndNinoPaperlessPreferenceForDefaultService =
      Map(
        sautr -> Future.successful(Some(PaperlessPreference(Map("default" -> PaperlessService(true, "genericTerms")), None))),
        nino -> Future.successful(Some(PaperlessPreference(Map("default" -> PaperlessService(true, "genericTerms")), None)))
      )

    def createService(withPreferences: Map[TaxIdWithName, Future[Option[PaperlessPreference]]])(withTaxIds: TaxIdWithName*): PaperlessActivateService = {
      new PaperlessActivateService {
        override def authorityConnector: AuthConnector = authConnector(withTaxIds)

        override def preferenceConnector: PreferencesConnector = preferencesConnector(withPreferences)
      }
    }

    def authConnector(taxIds: Seq[TaxIdWithName]) = {
      val authConnectorMock = mock[AuthConnector]
      when(authConnectorMock.currentTaxIdentifiers(any())).thenReturn(Future.successful(taxIds.toSet))
      authConnectorMock
    }

    def preferencesConnector(preferences: Map[TaxIdWithName, Future[Option[PaperlessPreference]]]) = {

      val preferencesConnectorMock = mock[PreferencesConnector]
      when(preferencesConnectorMock.getPreferencesStatus(argEq(sautr.name), argEq(sautr.value))(any())).thenReturn(preferences.get(sautr).getOrElse(Future.successful(None)))
      when(preferencesConnectorMock.getPreferencesStatus(argEq(nino.name), argEq(nino.value))(any())).thenReturn(preferences.get(nino).getOrElse(Future.successful(None)))

      when(preferencesConnectorMock.autoOptIn(any(), any(), any(), any())(any(), any())).thenReturn((): Unit)
      preferencesConnectorMock
    }
  }

}
