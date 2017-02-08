import java.net.URLEncoder
import java.util.UUID

import play.api.Application
import play.api.libs.json.Json
import play.api.libs.ws.{WSAPI, WSRequest}
import play.api.mvc.Results.EmptyContent
import uk.gov.hmrc.crypto.ApplicationCrypto._
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.domain.{SaUtr, TaxIdentifier}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.it.{ExternalService, ExternalServiceRunner, MicroServiceEmbeddedServer, ServiceSpec}
import uk.gov.hmrc.test.it.{AuthorisationProvider, FrontendCookieHelper, GovernmentGatewayAuthorisationProvider}
import uk.gov.hmrc.time.DateTimeUtils
import views.sa.prefs.helpers.DateFormat

import scala.concurrent.duration._

trait TestUser {
  def userId = "SA0055"

  val password = "testing123"

  val utr = GenerateRandom.utr()
  val nino = GenerateRandom.nino()

  private val wsCache: (Application) => WSAPI = Application.instanceCache[WSAPI]
  private def wsApi(implicit app : Application): WSAPI =  wsCache(app)

  def call(fullUrl: String): WSRequest = {
    import play.api.Play.current
    wsApi.client.url(fullUrl)
  }
}

trait PreferencesFrontEndServer extends ServiceSpec {

  protected val server = new PreferencesFrontendIntegrationServer("PREFERENCES_FRONTEND_IT_TESTS")

  class PreferencesFrontendIntegrationServer(override val testName: String) extends MicroServiceEmbeddedServer {

    override protected def additionalConfig = Map(
      "Dev.auditing.consumer.baseUri.port" -> externalServicePorts("datastream")
    )

    def localResource(path : String): String = {
        s"http://localhost:$servicePort$path"
    }

    override protected val externalServices: Seq[ExternalService] = externalServiceNames.map(ExternalServiceRunner.runFromJar(_))

    override protected def startTimeout: Duration = 300.seconds
  }

  def externalServiceNames: Seq[String] = {
    Seq(
      "datastream",
      "auth",
      "entity-resolver",
      "mailgun",
      "email",
      "preferences",
      "hmrc-email-renderer"
    )
  }

  class TestCase extends TestUser {

    val todayDate = DateFormat.longDateFormat(Some(DateTimeUtils.now.toLocalDate)).get.body

    def uniqueEmail = s"${UUID.randomUUID().toString}@email.com"

    def changedUniqueEmail = uniqueEmail

    def encryptAndEncode(value: String) = URLEncoder.encode(ApplicationCrypto.QueryParameterCrypto.encrypt(PlainText(value)).value, "UTF-8")

    def urlWithHostContext(url: String) = new {
      def apply(returnUrl: String = "", returnLinkText: String = "") = call(server.localResource(s"$url?returnUrl=${encryptAndEncode(returnUrl)}&returnLinkText=${encryptAndEncode(returnLinkText)}"))
    }

    def `/sa/print-preferences/assets/`(file : String) = call(server.localResource(s"/sa/print-preferences/assets/$file")).get()

    val `/paperless/resend-verification-email` = urlWithHostContext("/paperless/resend-verification-email")
    val `/paperless/manage` = urlWithHostContext("/paperless/manage")

    val payeFormTypeBody = Json.parse(s"""{"active":true}""")

    def `/preferences`(header: (String, String)) = new {
      def getPreference = call(server.externalResource("entity-resolver", "/preferences"))
        .withHeaders(header)
        .get

      def postPendingEmail(email: String) = call(server.externalResource("entity-resolver", "/preferences/pending-email"))
        .withHeaders(header)
        .put(Json.parse(s"""{"email":"$email"}"""))
    }

    val `/portal/preferences` = new {

      def getForUtr(utr: String) = call(server.externalResource("entity-resolver", s"/portal/preferences/sa/$utr")).get().futureValue

      def getForNino(nino: String) = call(server.externalResource("entity-resolver", s"/portal/preferences/paye/$nino")).get().futureValue
    }

    def `/preferences/terms-and-conditions`(header: (String, String)) = new {
      def postPendingEmail(pendingEmail: String) = call(server.externalResource("entity-resolver",
        s"/preferences/terms-and-conditions")).withHeaders(header).post(Json.parse(s"""{"generic":{"accepted":true}, "email":"$pendingEmail"}"""))

      def postOptOut = call(server.externalResource("entity-resolver",
        s"/preferences/terms-and-conditions")).withHeaders(header).post(Json.parse("""{"generic":{"accepted":false}}"""))
    }

    def `/entity-resolver/sa/:utr`(utr: String) = {
      val response = call(server.externalResource("entity-resolver", path = s"entity-resolver/sa/$utr")).get().futureValue
      response.status should be(200)
      (response.json \ "_id").as[String]
    }

    def `/entity-resolver/paye/:nino`(nino: String) = {
      val response = call(server.externalResource("entity-resolver", path = s"entity-resolver/paye/$nino")).get().futureValue
      response.status should be(200)
      (response.json \ "_id").as[String]
    }


    val `/preferences-admin/sa/individual` = new {
      def verifyEmailFor(entityId: String) = call(server.externalResource("preferences",
        s"/preferences-admin/$entityId/verify-email")).post(EmptyContent())

      def postExpireVerificationLink(entityId: String) = call(server.externalResource("preferences",
        s"/preferences-admin/$entityId/expire-email-verification-link")).post(EmptyContent())

      def postLegacyOptOut(entityId: String) = {
        call(server.externalResource("preferences", path = s"/preferences-admin/$entityId/legacy-opt-out"))
          .post(Json.parse("{}"))
      }

      def postLegacyOptIn(entityId: String, email: String) = {
        call(server.externalResource("preferences", path = s"/preferences-admin/${entityId}/legacy-opt-in/${email}"))
          .post(Json.parse("{}"))
      }
    }

    val `/preferences-admin/bounce-email` = new {
      def post(emailAddress: String) = call(server.externalResource("preferences",
        "/preferences-admin/bounce-email")).post(Json.parse( s"""{
             |"emailAddress": "$emailAddress"
             |}""".stripMargin))
    }

    val `/preferences-admin/sa/bounce-email-inbox-full` = new {
      def post(emailAddress: String) = call(server.externalResource("preferences",
        "/preferences-admin/bounce-email")).post(Json.parse( s"""{
             |"emailAddress": "$emailAddress",
             |"code": 552
             |}""".stripMargin))
    }

    def `/paperless/warnings` = urlWithHostContext("/paperless/warnings")()
  }

  trait TestCaseWithFrontEndAuthentication extends TestCase with FrontendCookieHelper {

    implicit val hc = HeaderCarrier()

    def authResource(path: String) = server.externalResource("auth", path)

    lazy val ggAuthHeaderWithUtr = createGGAuthorisationHeaderWithUtr(utr)
    lazy val ggAuthHeaderWithNino = createGGAuthorisationHeaderWithNino(nino)

    private lazy val ggAuthorisationHeaderWithUtr = GovernmentGatewayAuthorisationProvider(authResource, utr.value)
    private lazy val ggAuthorisationHeaderWithNino = GovernmentGatewayAuthorisationProvider(authResource, nino.value)
    private lazy val verifyAuthorisationHeader = AuthorisationProvider.forVerify(authResource)

    def createGGAuthorisationHeaderWithUtr(ids: TaxIdentifier*): (String, String) = ggAuthorisationHeaderWithUtr.createBearerTokenHeader(ids.toList).futureValue

    def createGGAuthorisationHeaderWithNino(ids: TaxIdentifier*): (String, String) = ggAuthorisationHeaderWithNino.createBearerTokenHeader(ids.toList).futureValue

    def createVerifyAuthorisationHeader(utr: TaxIdentifier): (String, String) = verifyAuthorisationHeader.create(utr).futureValue

    lazy val cookieWithUtr = cookieFor(ggAuthorisationHeaderWithUtr.createBearerToken(List(utr)).futureValue).futureValue
    lazy val cookieWithNino = cookieFor(ggAuthorisationHeaderWithUtr.createBearerToken(List(nino)).futureValue).futureValue

    def cookieForUtr(utr: SaUtr) = cookieFor(ggAuthorisationHeaderWithUtr.createBearerToken(List(utr)).futureValue)

    def cookieForTaxIdentifiers(taxIdentifiers: TaxIdentifier*) = cookieFor(ggAuthorisationHeaderWithUtr.createBearerToken(taxIdentifiers.toList).futureValue).futureValue

    val returnUrl = "/test/return/url"
    val returnLinkText = "Continue"

    val encryptedReturnUrl = URLEncoder.encode(QueryParameterCrypto.encrypt(PlainText(returnUrl)).value, "UTF-8")
    val encryptedReturnText = URLEncoder.encode(QueryParameterCrypto.encrypt(PlainText(returnLinkText)).value, "UTF-8")

    def `/paperless/activate`(taxIdentifier: TaxIdentifier)(additionalUserTaxIdentifiers: TaxIdentifier*) = new {

      private val url = call(server.localResource("/paperless/activate"))
        .withHeaders(createGGAuthorisationHeaderWithUtr(taxIdentifier +: additionalUserTaxIdentifiers: _*), cookieForTaxIdentifiers(taxIdentifier +: additionalUserTaxIdentifiers: _*))
        .withQueryString(
          "returnUrl" -> QueryParameterCrypto.encrypt(PlainText(returnUrl)).value,
          "returnLinkText" -> QueryParameterCrypto.encrypt(PlainText(returnLinkText)).value
        )

      private val formTypeBody = Json.parse("""{"active":true}""")

      def put() = url.put(formTypeBody)
    }
  }
}