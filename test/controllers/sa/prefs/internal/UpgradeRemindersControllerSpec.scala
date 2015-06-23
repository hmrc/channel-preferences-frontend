package controllers.sa.prefs.internal

import connectors.PreferencesConnector
import controllers.sa.prefs.AuthorityUtils._
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.JsString
import play.api.mvc.{Request, AnyContent}
import play.api.test.WithApplication
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{EventTypes, ExtendedDataEvent}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.test.{WithFakeApplication, UnitSpec}

import scala.concurrent.Future

class UpgradeRemindersControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  import org.mockito.Matchers.{any, eq => is}

  "create an audit event with upgrade decision" in new UpgradeTestCase {
    when(controller.preferencesConnector.upgradeTermsAndConditions(is(utr), is(true))(any())).thenReturn(Future.successful(true))

    await(controller.upgradeTermsAndConditions(utr, Some(Nino(nino)), true))

    val eventArg : ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
    verify(controller.auditConnector).sendEvent(eventArg.capture())(any(), any())

    private val value: ExtendedDataEvent = eventArg.getValue
    value.auditSource  shouldBe "preferences-frontend"
    value.auditType shouldBe EventTypes.Succeeded
    value.tags should contain ("transactionName" -> "Set Print Preference")
    value.detail \ "client" shouldBe JsString("PAYETAI")
    value.detail \ "nino" shouldBe JsString(nino)
    value.detail \ "utr" shouldBe JsString(utr.utr)
    value.detail \ "TandCsScope" shouldBe JsString("Generic")
    value.detail \ "TandCsVersion" shouldBe JsString("V1")
    value.detail \ "userConfirmedREadTandCs" shouldBe JsString("true")
    value.detail \ "journey" shouldBe JsString("GenericUpgrade")
    value.detail \ "digital" shouldBe JsString("true")
    value.detail \ "cohort" shouldBe JsString("TES_MVP")

  }

  trait UpgradeTestCase  {

    implicit val hc = HeaderCarrier()

    implicit val request = mock[Request[AnyContent]]

    val utr = SaUtr("testUtr")
    val nino = "CE123456A"

    val controller = new UpgradeRemindersController {
      override val preferencesConnector: PreferencesConnector = mock[PreferencesConnector]

      override val authConnector: AuthConnector = mock[AuthConnector]

      override val auditConnector: AuditConnector = mock[AuditConnector]
    }
  }



}
