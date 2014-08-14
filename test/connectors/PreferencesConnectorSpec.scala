package connectors

import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.domain.SaUtr
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.test.UnitSpec
import scala.concurrent.Future
import uk.gov.hmrc.play.connectors.HeaderCarrier

class PreferencesConnectorSpec extends UnitSpec with ScalaFutures {

  implicit val hc = new HeaderCarrier


  class TestPreferencesConnector extends PreferencesConnector {
    override def serviceUrl: String = "http://prefernces.service/"

    override def http: HttpGet with HttpPost = ???
  }

  "The getEmailAddres method" should {
    "return None for a 404" in {
      val preferenceConnector = preferencesConnector(Future.successful(HttpResponse(404)))
      preferenceConnector.getEmailAddress(SaUtr("1")).futureValue should be (None)
    }

    "return None when there is not an email preference" in {
      val preferenceConnector = preferencesConnector(Future.successful(HttpResponse(200, Some(Json.parse(
        """
          |{
          |  "digital": false
          |}
        """.stripMargin)))))
      preferenceConnector.getEmailAddress(SaUtr("1")).futureValue should be (None)
    }

    "return an email address when there is an email preference" in {
      val preferenceConnector = preferencesConnector(Future.successful(HttpResponse(200, Some(Json.parse(
        """
          |{
          |  "email": {
          |    "email" : "a@b.com"
          |  }
          |}
        """.stripMargin)))))
      preferenceConnector.getEmailAddress(SaUtr("1")).futureValue should be (Some("a@b.com"))
    }

    def preferencesConnector(returnFromDoGet: Future[HttpResponse]): TestPreferencesConnector = new TestPreferencesConnector {
      override def http = new HttpGet with HttpPost {
        protected def doFormPost(url: String, body: Map[String, Seq[String]])(implicit hc: HeaderCarrier) = ???
        protected def doPost[A](url: String, body: A)(implicit rds: Writes[A], hc: HeaderCarrier) = ???
        protected def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
          returnFromDoGet
        }
      }
    }
  }

  "The responseToEmailVerificationLinkStatus method" should {
    import EmailVerificationLinkResponse._
    lazy val preferenceConnector = new TestPreferencesConnector()

    "return ok if updateEmailValidationStatusUnsecured returns 200" in {
      val result = preferenceConnector.responseToEmailVerificationLinkStatus(Future.successful(HttpResponse(200)))
      result.futureValue shouldBe OK
    }

    "return ok if updateEmailValidationStatusUnsecured returns 204" in {
      val result = preferenceConnector.responseToEmailVerificationLinkStatus(Future.successful(HttpResponse(204)))
      result.futureValue shouldBe OK
    }

    "return error if updateEmailValidationStatusUnsecured returns 400" in {
      val result = preferenceConnector.responseToEmailVerificationLinkStatus(Future.failed(new BadRequestException("")))
      result.futureValue shouldBe ERROR
    }

    "return error if updateEmailValidationStatusUnsecured returns 404" in {
      val result = preferenceConnector.responseToEmailVerificationLinkStatus(Future.failed(new NotFoundException("")))
      result.futureValue shouldBe ERROR
    }

    "return error if updateEmailValidationStatusUnsecured returns 500" in {
      val result = preferenceConnector.responseToEmailVerificationLinkStatus(Future.failed(new Upstream5xxResponse("", 500, 500)))

      result.futureValue shouldBe ERROR
    }

    "return expired if updateEmailValidationStatusUnsecured returns 410" in {
      val result = preferenceConnector.responseToEmailVerificationLinkStatus(Future.failed(new Upstream4xxResponse("", 410, 500)))
      result.futureValue shouldBe EXPIRED
    }
  }
}
