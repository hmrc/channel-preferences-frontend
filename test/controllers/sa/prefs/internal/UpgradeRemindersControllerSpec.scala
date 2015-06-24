package controllers.sa.prefs.internal

import connectors.PreferencesConnector
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{any, eq => is}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.JsString
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{EventTypes, ExtendedDataEvent}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future

class UpgradeRemindersControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  "create an audit event with positive upgrade decision" in new UpgradeTestCase {
    val digital = true
    val event = upgradeAndCaptureAuditEvent(digital).getValue

    event.auditSource  shouldBe "preferences-frontend"
    event.auditType shouldBe EventTypes.Succeeded
    event.tags should contain ("transactionName" -> "Set Print Preference")
    event.detail \ "client" shouldBe JsString("PAYETAI")
    event.detail \ "nino" shouldBe JsString(nino.value)
    event.detail \ "utr" shouldBe JsString(utr.utr)
    event.detail \ "TandCsScope" shouldBe JsString("Generic")
    event.detail \ "TandCsVersion" shouldBe JsString("V1")
    event.detail \ "userConfirmedReadTandCs" shouldBe JsString("true")
    event.detail \ "journey" shouldBe JsString("GenericUpgrade")
    event.detail \ "digital" shouldBe JsString("true")
    event.detail \ "cohort" shouldBe JsString("TES_MVP")

  }

  "create an audit event with negative upgrade decision" in new UpgradeTestCase {
    val digital = false
    val event = upgradeAndCaptureAuditEvent(digital).getValue

    event.auditSource  shouldBe "preferences-frontend"
    event.auditType shouldBe EventTypes.Succeeded
    event.tags should contain ("transactionName" -> "Set Print Preference")
    event.detail \ "client" shouldBe JsString("PAYETAI")
    event.detail \ "nino" shouldBe JsString(nino.value)
    event.detail \ "utr" shouldBe JsString(utr.utr)
    event.detail \ "TandCsScope" shouldBe JsString("Generic")
    event.detail \ "TandCsVersion" shouldBe JsString("V1")
    event.detail \ "userConfirmedReadTandCs" shouldBe JsString("true")
    event.detail \ "journey" shouldBe JsString("GenericUpgrade")
    event.detail \ "digital" shouldBe JsString("false")
    event.detail \ "cohort" shouldBe JsString("TES_MVP")

  }

// TODO...WHAT SHOULD BE DONE CONCERNING SENDING A REQUEST TO THE POST URL IF THE GET HAS NOT BEEN CALLED>

  "the validate upgrade form" should {
    "redirect to supplied url if terms and conditions are checked and digital button pressed " in new UpgradeTestCase {

      when(controller.preferencesConnector.upgradeTermsAndConditions(is(utr), is(true))(any())).thenReturn(Future.successful(true))

      val result = await(controller.validateUpgradeForm("someurl", utr, Some(nino))(testRequest.withFormUrlEncodedBody("submitButton" -> "digital", "accept-tc" -> "true")))

      status(result) shouldBe 303
      header("Location", result).get should include("someurl")
    }

    "return bad request if terms and conditions are not checked and digital button pressed" in new UpgradeTestCase {

      val result = await(controller.validateUpgradeForm("someurl", utr, Some(nino))(testRequest.withFormUrlEncodedBody("submitButton" -> "digital")))

      status(result) shouldBe 400
    }

    "redirect to supplied url if non-digital button pressed " in new UpgradeTestCase {

      when(controller.preferencesConnector.upgradeTermsAndConditions(is(utr), is(false))(any())).thenReturn(Future.successful(true))

      val result = await(controller.validateUpgradeForm("someurl", utr, Some(nino))(testRequest.withFormUrlEncodedBody("submitButton" -> "non-digital")))

      status(result) shouldBe 303
      header("Location", result).get should include("someurl")
    }
  }

  trait UpgradeTestCase  {

    implicit val hc = HeaderCarrier()
    implicit val request = mock[Request[AnyContent]]

    val testRequest = FakeRequest()
    val utr = SaUtr("testUtr")
    val nino = Nino("CE123456A")

    val controller = new UpgradeRemindersController {
      override val preferencesConnector: PreferencesConnector = mock[PreferencesConnector]
      override val authConnector: AuthConnector = mock[AuthConnector]
      override val auditConnector: AuditConnector = mock[AuditConnector]
    }

    def upgradeAndCaptureAuditEvent(digital: Boolean):ArgumentCaptor[ExtendedDataEvent] = {
      when(controller.preferencesConnector.upgradeTermsAndConditions(is(utr), is(digital))(any())).thenReturn(Future.successful(true))
      await(controller.upgradeTermsAndConditions(utr, Some(nino), digital))
      val eventArg : ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      verify(controller.auditConnector).sendEvent(eventArg.capture())(any(), any())
      return  eventArg
    }
  }
}
