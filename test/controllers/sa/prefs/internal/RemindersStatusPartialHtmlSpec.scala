package controllers.sa.prefs.internal

import connectors.SaEmailPreference.Status
import connectors.{PreferencesConnector, SaEmailPreference, SaPreference}
import controllers.sa.prefs.AuthorityUtils._
import org.scalatest.concurrent.ScalaFutures
import play.api.test.FakeRequest
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.connectors.HeaderCarrier
import uk.gov.hmrc.play.http.{HttpPut, HttpPost, HttpGet}
import uk.gov.hmrc.play.test.{WithFakeApplication, WithHeaderCarrier}
import uk.gov.hmrc.test.UnitSpec
import scala.concurrent.Future

class RemindersStatusPartialHtmlSpec extends UnitSpec with WithHeaderCarrier with WithFakeApplication with ScalaFutures {

  "Reminders Partial Html" should {
    "contain the email reminder title for a sa user" in pending

    "contain resend-validation-email option for a pending email in preference" in new TestCase {
      val emailPreferences: SaEmailPreference = SaEmailPreference("test@test.com", Status.pending, false)
      val saPreference: SaPreference = SaPreference(true, Some(emailPreferences))

      implicit val request = FakeRequest("GET", "/portal/sa/123456789")
      implicit val hc = new HeaderCarrier()
      implicit val saUser = User(userId = "userId", userAuthority = saAuthority("userId", "1234567890"))

      val partialHtml = new PartialHtml(saPreference).detailsStatus().futureValue

      partialHtml.body should (
        include(emailPreferences.email) and
        include("Send verification email") and
        include("/account/account-details/sa/resend-validation-email") and
        include("/account/account-details/sa/update-email-address")
      )
    }
  }

}

class TestCase{

  class PartialHtml(saPreference: SaPreference) extends RemindersStatusPartialHtml {

    override val preferencesConnector = new PreferencesConnector {
      override def http: HttpGet with HttpPost with HttpPut = ???

      override def serviceUrl: String = ???

      override def getPreferences(utr: SaUtr)(implicit headerCarrier: HeaderCarrier): Future[Option[SaPreference]] = Future.successful(Some(saPreference))
    }
  }

}
