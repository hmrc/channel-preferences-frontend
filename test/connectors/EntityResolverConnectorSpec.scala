package connectors

import connectors.SaEmailPreference.Status
import helpers.ConfigHelper
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.{JsValue, Json, Writes}
import uk.gov.hmrc.circuitbreaker.UnhealthyServiceException
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.test.UnitSpec
import PreferenceResponse._
import model.HostContext

import scala.concurrent.Future
import uk.gov.hmrc.http.{ BadRequestException, HeaderCarrier, HttpGet, HttpPost, HttpPut, HttpResponse, InternalServerException, NotFoundException, Upstream4xxResponse, Upstream5xxResponse }
import uk.gov.hmrc.http.hooks.HttpHook

class EntityResolverConnectorSpec extends UnitSpec with ScalaFutures with OneAppPerSuite with MockitoSugar {

  implicit val hc = new HeaderCarrier
  implicit val hostContext : HostContext = new HostContext(returnUrl = "", returnLinkText = "")
  override implicit lazy val app : Application = ConfigHelper.fakeApp

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
    val serviceUrl = "http://entity-resolver.service/"
    def http: HttpGet with HttpPost with HttpPut = ???
  }

  def entityResolverConnector(returnFromDoGet: String => Future[HttpResponse] = defaultGetHandler,
                              returnFromDoPost: (String, Any) => Future[HttpResponse] = defaultPostHandler,
                              returnFromDoPut: (String, Any) => Future[HttpResponse] = defaultPutHandler) = new TestPreferencesConnector {
    override val http = new HttpGet with HttpPost with HttpPut with AppName with HttpAuditing {
      def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = returnFromDoGet(url)

      def doPostString(url: String, body: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

      def doFormPost(url: String, body: Map[String, Seq[String]])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

      def doPost[A](url: String, body: A, headers: Seq[(String, String)])(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = returnFromDoPost(url, body)

      def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

      def doPut[A](url: String, body: A)(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = returnFromDoPut(url, body)

      val hooks: Seq[HttpHook] = Seq(AuditingHook)

      def auditConnector = ???

      override def configuration = ???
    }
  }

  "getPreferencesStatusByToken" should {
    val GOOD_SERVICE = "mtdfbit"
    val BAD_SERVICE = "rubbish"
    val GOOD_TOKEN = "91abdbb1-6ad4-4419-8f33-a7ea6cf8e388"
    val BAD_TOKEN = "rubbish"

    "map no preference to PreferenceNotFound with no email" in {
      val connector = entityResolverConnector(
        url => Future.successful(HttpResponse(responseStatus = NOT_FOUND, responseString = Some("Preference not found")))
      )

      connector.getPreferencesStatusByToken(GOOD_SERVICE, GOOD_TOKEN).futureValue shouldBe Right(PreferenceNotFound(None))
    }
  }

  "getPreferencesStatus" should {

    implicit val hc = HeaderCarrier()

    "map no preference to PreferenceNotFound with no email" in {
      val connector = entityResolverConnector(
        url => Future.successful(HttpResponse(responseStatus = NOT_FOUND, responseString = Some("Preference not found")))
      )

      connector.getPreferencesStatus().futureValue shouldBe Right(PreferenceNotFound(None))
    }

    "map found paperless preference to true" in {
      val connector = entityResolverConnector(
        url =>   Future.successful(HttpResponse(200, Some(Json.parse(
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
      )

      val expectedResult = new SaPreference(digital = true, email = Some(new SaEmailPreference("test@mail.com", SaEmailPreference.Status.Verified)))

      val preferenceResponse = connector.getPreferencesStatus().futureValue
      preferenceResponse shouldBe Right(PreferenceFound(true, expectedResult.toNewPreference().email))
    }

    "map found non-paperless preference to false" in {
      val connector = entityResolverConnector(
        url =>   Future.successful(HttpResponse(200, Some(Json.parse(
          """
            |{
            |   "digital": false
            |}
          """.stripMargin))))
      )

      val expectedResult = new SaPreference(digital = false, email = None)

      connector.getPreferencesStatus().futureValue shouldBe Right(PreferenceFound(expectedResult.toNewPreference().termsAndConditions.get("generic").get.accepted, None))
    }

    "map an auth failure to UNAUTHORIZED" in {
      val connector = entityResolverConnector(url => Future.successful(HttpResponse(responseStatus = UNAUTHORIZED, responseString = Some("Preference not found")))
      )

      connector.getPreferencesStatus().futureValue shouldBe Left(UNAUTHORIZED)
    }

    "circuit breaker configuration should be applied and unhealthy service exception will kick in after 5th failed call to preferences" in {
      val connector = entityResolverConnector(
        url => Future.successful(HttpResponse(INTERNAL_SERVER_ERROR))
      )

      1 to 5 foreach { _ =>
        connector.getPreferencesStatus().failed.futureValue shouldBe an[Upstream5xxResponse]
      }
      connector.getPreferencesStatus().failed.futureValue shouldBe an[UnhealthyServiceException]
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

      val preferences = preferenceConnector.getPreferences().futureValue

      preferences shouldBe Some(SaPreference(
        digital = true, email = Some(SaEmailPreference(
          email = "test@mail.com",
          status = Status.Verified))
      ).toNewPreference())
    }

    "return None for a 404" in {
      val preferenceConnector = entityResolverConnector(_ => Future.successful(HttpResponse(404, None)))

      val preferences = preferenceConnector.getPreferences().futureValue

      preferences shouldBe None
    }

    "return None for a 410" in {
      val preferenceConnector = entityResolverConnector(_ => Future.successful(HttpResponse(410, None)))

      val preferences = preferenceConnector.getPreferences().futureValue

      preferences shouldBe None
    }

    "circuit breaker configuration should be applied and unhealthy service exception will kick in after 5th failed call to preferences" in {
      val connector = entityResolverConnector(
        returnFromDoGet = _ => Future.failed(new InternalServerException("some exception"))
      )

      1 to 5 foreach { _ =>
        connector.getPreferences().failed.futureValue shouldBe an[InternalServerException]
      }
      connector.getPreferences().failed.futureValue shouldBe an[UnhealthyServiceException]
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
    lazy val preferenceConnector = new TestPreferencesConnector()

    "return ok if updateEmailValidationStatusUnsecured returns 200" in {
      val result = preferenceConnector.responseToEmailVerificationLinkStatus(Future.successful(HttpResponse(200)))
      result.futureValue shouldBe Validated
    }

    "return ok if updateEmailValidationStatusUnsecured returns 204" in {
      val result = preferenceConnector.responseToEmailVerificationLinkStatus(Future.successful(HttpResponse(204)))
      result.futureValue shouldBe Validated
    }

    "return ok with the return link text and return url if updateEmailValidationStatusUnsecured returns 201" in {
      val responseJson = Json.parse(
        """{
          |     "returnLinkText": "Return Link Text",
          |     "returnUrl": "ReturnUrl"
          |}""".stripMargin)
      val result = preferenceConnector.responseToEmailVerificationLinkStatus(Future.successful(HttpResponse(201, responseJson = Some(responseJson))))
      result.futureValue shouldBe ValidatedWithReturn("Return Link Text", "ReturnUrl")
    }

    "return error if updateEmailValidationStatusUnsecured returns 400" in {
      val result = preferenceConnector.responseToEmailVerificationLinkStatus(Future.failed(new BadRequestException("")))
      result.futureValue shouldBe ValidationError
    }

    "return 'error with return' with error if updateEmailValidationStatusUnsecured returns 412" in {
      val result = preferenceConnector.responseToEmailVerificationLinkStatus(Future.failed(Upstream4xxResponse("""PUT of something Response body: '{"returnLinkText":"a message", "returnUrl": "https://some/place"}'""", PRECONDITION_FAILED, 0, Map())))
      result.futureValue shouldBe ValidationErrorWithReturn("a message", "https://some/place")
    }

    "return error if updateEmailValidationStatusUnsecured returns 404" in {
      val result = preferenceConnector.responseToEmailVerificationLinkStatus(Future.failed(new NotFoundException("")))
      result.futureValue shouldBe ValidationError
    }

    "pass through the failure if updateEmailValidationStatusUnsecured returns 500" in {
      val expectedErrorResponse = Upstream5xxResponse("", 500, 500)
      val result = preferenceConnector.responseToEmailVerificationLinkStatus(Future.failed(expectedErrorResponse))

      result.failed.futureValue shouldBe expectedErrorResponse
    }

    "return expired if updateEmailValidationStatusUnsecured returns 410" in {
      val result = preferenceConnector.responseToEmailVerificationLinkStatus(Future.failed(Upstream4xxResponse("", 410, 500)))
      result.futureValue shouldBe ValidationExpired
    }

    "return wrong token if updateEmailValidationStatusUnsecured returns 409" in {
      val result = preferenceConnector.responseToEmailVerificationLinkStatus(Future.failed(Upstream4xxResponse("", 409, 500)))
      result.futureValue shouldBe WrongToken
    }
  }

  "The updateTermsAndConditions method" should {
    trait PayloadCheck {
      def status: Int = 200
      def expectedPayload: TermsAndConditionsUpdate
      def postedPayload(payload: TermsAndConditionsUpdate) = payload should be (expectedPayload)

      val connector = entityResolverConnector(returnFromDoPost = checkPayloadAndReturn)

      def checkPayloadAndReturn(url: String, requestBody: Any): Future[HttpResponse] = {
        postedPayload(requestBody.asInstanceOf[JsValue].as[TermsAndConditionsUpdate])
        Future.successful(HttpResponse(status))
      }
    }

    "send generic accepted true and return preferences created if terms and conditions are accepted and updated and preferences created" in new PayloadCheck {
      override val expectedPayload = TermsAndConditionsUpdate(generic = Some(TermsAccepted(true)), taxCredits = None, email = None)

      connector.updateTermsAndConditions(GenericTerms -> TermsAccepted(true), email = None).futureValue should be (PreferencesExists)
    }

    "send generic accepted false and return preferences created if terms and conditions are not accepted and updated and preferences created" in new PayloadCheck {
      override val expectedPayload = TermsAndConditionsUpdate(generic = Some(TermsAccepted(false)), taxCredits = None, email = None)

      connector.updateTermsAndConditions(GenericTerms -> TermsAccepted(false), email = None).futureValue should be (PreferencesExists)
    }

    "send taxCredits accepted true and return preferences created if terms and conditions are accepted and updated and preferences created" in new PayloadCheck {
      override val expectedPayload = TermsAndConditionsUpdate(generic = None, taxCredits = Some(TermsAccepted(true)), email = None)

      connector.updateTermsAndConditions(TaxCreditsTerms -> TermsAccepted(true), email = None).futureValue should be (PreferencesExists)
    }

    "send taxCredits accepted false and return preferences created if terms and conditions are not accepted and updated and preferences created" in new PayloadCheck {
      override val expectedPayload = TermsAndConditionsUpdate(generic = None, taxCredits = Some(TermsAccepted(false)), email = None)

      connector.updateTermsAndConditions(TaxCreditsTerms -> TermsAccepted(false), email = None).futureValue should be (PreferencesExists)
    }

    "include the returnUrl and returnLinkText in the post when called by a service and token" in new PayloadCheck {
      override val expectedPayload = TermsAndConditionsUpdate(generic = None, taxCredits = Some(TermsAccepted(true)), email = None, returnUrl = Some("return Url"), returnText = Some("return link text"))
      implicit val hostContext : HostContext = new HostContext(returnUrl = "return Url", returnLinkText = "return link text")

      connector.updateTermsAndConditionsForSvc(TaxCreditsTerms -> TermsAccepted(true), email = None, svc = Some("MTDFBIT"), token = Some("A TOKEN"), includeLinkDetails = true).futureValue should be (PreferencesExists)
    }

    "return failure if any problems" in new PayloadCheck {
      override val status = 401
      override val expectedPayload = TermsAndConditionsUpdate(generic = Some(TermsAccepted(true)), taxCredits = None, email = None)

      whenReady(connector.updateTermsAndConditions(GenericTerms -> TermsAccepted(true), email = None).failed) {
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
        postedPayload(requestBody.asInstanceOf[JsValue].as[TermsAndConditionsUpdate])
        Future.successful(HttpResponse(status))
      }
    }

    "send generic accepted true with email" in new NewUserPayloadCheck {
      override def expectedPayload = TermsAndConditionsUpdate(generic = Some(TermsAccepted(true)), taxCredits = None, Some(email))

      connector.updateTermsAndConditions(GenericTerms -> TermsAccepted(true), Some(email)).futureValue should be (PreferencesCreated)
    }

    "send generic accepted false with no email" in new NewUserPayloadCheck {
      override def expectedPayload = TermsAndConditionsUpdate(generic = Some(TermsAccepted(false)), taxCredits = None, None)

      connector.updateTermsAndConditions(GenericTerms -> TermsAccepted(false), None).futureValue should be (PreferencesCreated)
    }

    "send taxCredits accepted true with email" in new NewUserPayloadCheck {
      override def expectedPayload = TermsAndConditionsUpdate(generic = Some(TermsAccepted(true)), taxCredits = None, Some(email))

      connector.updateTermsAndConditions(GenericTerms -> TermsAccepted(true), Some(email)).futureValue should be (PreferencesCreated)
    }

    "send taxCredits accepted false with no email" in new NewUserPayloadCheck {
      override def expectedPayload = TermsAndConditionsUpdate(generic = Some(TermsAccepted(false)), taxCredits = None, None)

      connector.updateTermsAndConditions(GenericTerms -> TermsAccepted(false), None).futureValue should be (PreferencesCreated)
    }

    "try and send accepted true with email where preferences not working" in new NewUserPayloadCheck {
      override def expectedPayload = TermsAndConditionsUpdate(generic = Some(TermsAccepted(true)), taxCredits = None, Some(email))

      override def status: Int = 401

      whenReady(connector.updateTermsAndConditions(GenericTerms -> TermsAccepted(true), Some(email)).failed) {
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
