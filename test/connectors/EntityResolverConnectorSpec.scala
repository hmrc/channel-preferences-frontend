package connectors

import java.net.URLEncoder

import connectors.SaEmailPreference.Status
import model.{HostContext, NoticeOfCoding, SaAll}
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.hooks.HttpHook
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future

class EntityResolverConnectorSpec extends UnitSpec with ScalaFutures with WithFakeApplication {

  implicit val hc = new HeaderCarrier

  import EntityResolverConnector._

  private def defaultGetHandler: (String) => Future[AnyRef with HttpResponse] = {
    _ => Future.successful(HttpResponse(200))
  }

  private def defaultPostHandler: (String, Any) => Future[AnyRef with HttpResponse] = {
    (a, b) => Future.successful(HttpResponse(200))
  }

  private def defaultPutHandler: (String, Any) => Future[AnyRef with HttpResponse] = {
    (a, b) => Future.successful(HttpResponse(200))
  }

  class TestPreferencesConnector extends EntityResolverConnector with ServicesConfig {

    override def serviceUrl: String = "http://entity-resolver.service/"

    override def http: HttpGet with HttpPost with HttpPut = ???
  }

  def entityResolverConnector(returnFromDoGet: String => Future[HttpResponse] = defaultGetHandler,
                              returnFromDoPost: (String, Any) => Future[HttpResponse] = defaultPostHandler,
                              returnFromDoPut: (String, Any) => Future[HttpResponse] = defaultPutHandler) = new TestPreferencesConnector {
    override def http = new HttpGet with HttpPost with HttpPut with AppName with HttpAuditing {
      override protected def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = returnFromDoGet(url)

      override protected def doPostString(url: String, body: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

      override protected def doFormPost(url: String, body: Map[String, Seq[String]])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

      override protected def doPost[A](url: String, body: A, headers: Seq[(String, String)])(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = returnFromDoPost(url, body)

      override protected def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

      override protected def doPut[A](url: String, body: A)(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = returnFromDoPut(url, body)

      val hooks: Seq[HttpHook] = Seq(AuditingHook)

      def auditConnector = ???
    }
  }

  "activate" should {

    def urlEncode(text: String) = URLEncoder.encode(text, "UTF-8")

    def toUrlParams(hostContext: HostContext) = s"returnUrl=${urlEncode(hostContext.returnUrl)}&returnLinkText=${urlEncode(hostContext.returnLinkText)}"

    "proxy to the /preferences/sa/individual/:utr/activations/sa-all if sa-all form type given" in {
      implicit val hc = HeaderCarrier()

      val utr = "12345"
      val hostContext = HostContext(returnUrl = "some/return/url", returnLinkText = "continue")
      val payload = """{"active":true}"""

      val preferenceResponseStatus = PRECONDITION_FAILED
      val preferenceResponseBody = "link/to/preferences-frontend"

      val connector = entityResolverConnector(
        returnFromDoPut = (url, body) => {
          url should include(s"/preferences/sa/individual/$utr/activations/sa-all?${toUrlParams(hostContext)}")
          body.toString should be (payload)
          Future.successful(HttpResponse(responseStatus = preferenceResponseStatus, responseString = Some(preferenceResponseBody)))
      })

      connector.activate(SaAll, utr, hostContext, Json.parse(payload)).futureValue should be (ActivationResponse(preferenceResponseStatus, preferenceResponseBody))
    }

    "proxy to the /preferences/paye/individual/:nino/activations/notice-of-coding if notice-of-coding form type given" in {
      implicit val hc = HeaderCarrier()

      val nino = "ABCD"
      val hostContext = HostContext(returnUrl = "some/return/url", returnLinkText = "continue")
      val payload = """{"active":true}"""

      val preferenceResponseStatus = PRECONDITION_FAILED
      val preferenceResponseBody = "link/to/preferences-frontend"

      val connector = entityResolverConnector(
        returnFromDoPut = (url, body) => {
          url should include(s"/preferences/paye/individual/$nino/activations/notice-of-coding?${toUrlParams(hostContext)}")
          body.toString should be (payload)
          Future.successful(HttpResponse(responseStatus = preferenceResponseStatus, responseString = Some(preferenceResponseBody)))
      })

      connector.activate(NoticeOfCoding, nino, hostContext, Json.parse(payload)).futureValue should be (ActivationResponse(preferenceResponseStatus, preferenceResponseBody))
    }

    "return ActivationResponse if exception thrown on PUT" in {
      implicit val hc = HeaderCarrier()

      val nino = "ABCD"
      val hostContext = HostContext(returnUrl = "some/return/url", returnLinkText = "continue")
      val payload = """{"active":true}"""

      val connector = entityResolverConnector(returnFromDoPut = (url, body) => Future.failed(new InternalServerException("some exception")))

      connector.activate(NoticeOfCoding, nino, hostContext, Json.parse(payload)).futureValue.status should be (INTERNAL_SERVER_ERROR)
    }
  }

  "The getPreferences method" should {
    val nino = Nino("CE123457D")

    "return the preferences for utr only" in {
      val preferenceConnector = entityResolverConnector { url =>
        url should not include nino.value

        Future.successful(HttpResponse(200, Some(Json.parse(
          """
            |{
            |   "digital": true,
            |   "email": {
            |     "email": "test@mail.com",
            |     "status": "verified",
            |     "mailboxFull": false
            |   }
            |}
          """.stripMargin))))
      }

      val preferences = preferenceConnector.getPreferences(SaUtr("1")).futureValue

      preferences shouldBe Some(SaPreference(
        digital = true, email = Some(SaEmailPreference(
          email = "test@mail.com",
          status = Status.Verified))
      ))
    }

    "return None for a 404" in {
      val preferenceConnector = entityResolverConnector(_ => Future.successful(HttpResponse(404, None)))

      val preferences = preferenceConnector.getPreferences(SaUtr("1")).futureValue

      preferences shouldBe None
    }

    "return None for a 410" in {
      val preferenceConnector = entityResolverConnector(_ => Future.successful(HttpResponse(410, None)))

      val preferences = preferenceConnector.getPreferences(SaUtr("1")).futureValue

      preferences shouldBe None
    }
  }

  "The getEmailAddress method" should {
    "return None for a 404" in {
      val preferenceConnector = entityResolverConnector(_ => Future.successful(
        HttpResponse(responseStatus = 404, responseJson = Some(Json.obj("reason" -> "EMAIL_ADDRESS_NOT_VERIFIED")))))
      preferenceConnector.getEmailAddress(SaUtr("1")).futureValue should be(None)
    }

    "return Error for other status code" in {
      val preferenceConnector = entityResolverConnector(_ => Future.successful(HttpResponse(400)))
      preferenceConnector.getEmailAddress(SaUtr("1")).failed.futureValue should be(an[Exception])
    }

    "return an email address when there is an email preference" in {
      val preferenceConnector = entityResolverConnector(_ => Future.successful(HttpResponse(200, Some(Json.parse(
        """{
          |  "email" : "a@b.com"
          |}
        """.stripMargin)))))
      preferenceConnector.getEmailAddress(SaUtr("1")).futureValue should be(Some("a@b.com"))
    }
  }

  "The responseToEmailVerificationLinkStatus method" should {
    import connectors.EmailVerificationLinkResponse._
    lazy val preferenceConnector = new TestPreferencesConnector()

    "return ok if updateEmailValidationStatusUnsecured returns 200" in {
      val result = preferenceConnector.responseToEmailVerificationLinkStatus(Future.successful(HttpResponse(200)))
      result.futureValue shouldBe Ok
    }

    "return ok if updateEmailValidationStatusUnsecured returns 204" in {
      val result = preferenceConnector.responseToEmailVerificationLinkStatus(Future.successful(HttpResponse(204)))
      result.futureValue shouldBe Ok
    }

    "return error if updateEmailValidationStatusUnsecured returns 400" in {
      val result = preferenceConnector.responseToEmailVerificationLinkStatus(Future.failed(new BadRequestException("")))
      result.futureValue shouldBe Error
    }

    "return error if updateEmailValidationStatusUnsecured returns 404" in {
      val result = preferenceConnector.responseToEmailVerificationLinkStatus(Future.failed(new NotFoundException("")))
      result.futureValue shouldBe Error
    }

    "pass through the failure if updateEmailValidationStatusUnsecured returns 500" in {
      val expectedErrorResponse = Upstream5xxResponse("", 500, 500)
      val result = preferenceConnector.responseToEmailVerificationLinkStatus(Future.failed(expectedErrorResponse))

      result.failed.futureValue shouldBe expectedErrorResponse
    }

    "return expired if updateEmailValidationStatusUnsecured returns 410" in {
      val result = preferenceConnector.responseToEmailVerificationLinkStatus(Future.failed(Upstream4xxResponse("", 410, 500)))
      result.futureValue shouldBe Expired
    }

    "return wrong token if updateEmailValidationStatusUnsecured returns 409" in {
      val result = preferenceConnector.responseToEmailVerificationLinkStatus(Future.failed(Upstream4xxResponse("", 409, 500)))
      result.futureValue shouldBe WrongToken
    }
  }

  "The upgradeTermsAndConditions method" should {
    trait PayloadCheck {
      def status: Int = 200
      def expectedPayload: TermsAndConditionsUpdate
      def postedPayload(payload: TermsAndConditionsUpdate) = payload should be (expectedPayload)

      val connector = entityResolverConnector(returnFromDoPost = checkPayloadAndReturn)

      def checkPayloadAndReturn(url: String, requestBody: Any): Future[HttpResponse] = {
        postedPayload(requestBody.asInstanceOf[TermsAndConditionsUpdate])
        Future.successful(HttpResponse(status))
      }
    }

    "send accepted true and return preferences created if terms and conditions are accepted and updated and preferences created" in new PayloadCheck {
      override val expectedPayload = TermsAndConditionsUpdate(TermsAccepted(true), email = None)

      connector.updateTermsAndConditions(SaUtr("testing"), Generic -> TermsAccepted(true), email = None).futureValue should be (PreferencesExists)
    }

    "send accepted false and return preferences created if terms and conditions are not accepted and updated and preferences created" in new PayloadCheck {
      override val expectedPayload = TermsAndConditionsUpdate(TermsAccepted(false), email = None)

      connector.updateTermsAndConditions(SaUtr("testing"), Generic -> TermsAccepted(false), email = None).futureValue should be (PreferencesExists)
    }

    "return failure if any problems" in new PayloadCheck {
      override val status = 401
      override val expectedPayload = TermsAndConditionsUpdate(TermsAccepted(true), email = None)

      whenReady(connector.updateTermsAndConditions(SaUtr("testing"), Generic -> TermsAccepted(true), email = None).failed) {
        case e => e shouldBe an[Upstream4xxResponse]
      }
    }
  }

  "New user" should {
    trait NewUserPayloadCheck {
      def status: Int = 201
      def expectedPayload: TermsAndConditionsUpdate
      def postedPayload(payload: TermsAndConditionsUpdate) = payload should be (expectedPayload)
      val email = "test@test.com"

      val connector = entityResolverConnector(returnFromDoPost = checkPayloadAndReturn)

      def checkPayloadAndReturn(url: String, requestBody: Any): Future[HttpResponse] = {
        postedPayload(requestBody.asInstanceOf[TermsAndConditionsUpdate])
        Future.successful(HttpResponse(status))
      }
    }

    "send accepted true with email" in new NewUserPayloadCheck {
      override def expectedPayload = TermsAndConditionsUpdate(TermsAccepted(true), Some(email))

      connector.updateTermsAndConditions(SaUtr("test"), Generic -> TermsAccepted(true), Some(email)).futureValue should be (PreferencesCreated)
    }

    "send accepted false with no email" in new NewUserPayloadCheck {
      override def expectedPayload = TermsAndConditionsUpdate(TermsAccepted(false), None)

      connector.updateTermsAndConditions(SaUtr("test"), Generic -> TermsAccepted(false), None).futureValue should be (PreferencesCreated)
    }

    "try and send accepted true with email where preferences not working" in new NewUserPayloadCheck {
      override def expectedPayload = TermsAndConditionsUpdate(TermsAccepted(true), Some(email))

      override def status: Int = 401

      whenReady(connector.updateTermsAndConditions(SaUtr("test"), Generic -> TermsAccepted(true), Some(email)).failed) {
        case e => e shouldBe an[Upstream4xxResponse]
      }
    }
  }

  "Activate new user" should {
    trait ActivateUserPayloadCheck {
      def status: Int = 200
      def expectedPayload: ActivationStatus = ActivationStatus(true)
      def putPayload(payload: ActivationStatus) = payload should be(expectedPayload)

      val connector = entityResolverConnector(returnFromDoPut = checkPayloadAndReturn)
      val returnUrl = "/any/old/url"

      def checkPayloadAndReturn(url: String, requestBody: Any): Future[HttpResponse] = {
        putPayload(requestBody.asInstanceOf[ActivationStatus])
        Future.successful(HttpResponse(status))
      }
    }
  }
}