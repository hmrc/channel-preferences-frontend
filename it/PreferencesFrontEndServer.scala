import java.net.URLEncoder
import java.util.UUID

import play.api.Play.current
import play.api.libs.json.Json
import play.api.libs.ws.WS
import play.api.mvc.Results.EmptyContent
import uk.gov.hmrc.crypto.ApplicationCrypto._
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.domain.{Nino, SaUtr, TaxIdentifier}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.it.{ExternalService, MicroServiceEmbeddedServer, ServiceSpec}
import uk.gov.hmrc.test.it.{AuthorisationProvider, FrontendCookieHelper}
import uk.gov.hmrc.time.DateTimeUtils
import views.sa.prefs.helpers.DateFormat

import scala.concurrent.duration._

trait TestUser {
  def userId = "SA0055"

  val password = "testing123"

  val utr = GenerateRandom.utr().value
  val nino = GenerateRandom.nino()
}

trait PreferencesFrontEndServer extends ServiceSpec {
  protected val server = new PreferencesFrontendIntegrationServer("PREFERENCES_FRONTEND_IT_TESTS")

  class PreferencesFrontendIntegrationServer(override val testName: String) extends MicroServiceEmbeddedServer {

    override protected def additionalConfig = Map(
      "Dev.auditing.consumer.baseUri.port" -> externalServicePorts("datastream")
    )

    override protected val externalServices: Seq[ExternalService] = externalServiceNames.map(ExternalService.runFromJar(_))

    override protected def startTimeout: Duration = 300.seconds
  }

  def externalServiceNames: Seq[String] = {
    Seq(
      "datastream",
      "external-government-gateway",
      "government-gateway",
      "auth",
      "message",
      "entity-resolver",
      "mailgun",
      "hmrc-deskpro",
      "ca-frontend",
      "email",
      "cid",
      "preferences"
    )
  }

  class TestCase extends TestUser {

    val todayDate = DateFormat.longDateFormat(Some(DateTimeUtils.now.toLocalDate)).get.body

    def uniqueEmail = s"${UUID.randomUUID().toString}@email.com"

    def changedUniqueEmail = uniqueEmail

    def encryptAndEncode(value: String) = URLEncoder.encode(ApplicationCrypto.QueryParameterCrypto.encrypt(PlainText(value)).value, "UTF-8")

    def urlWithHostContext(url: String) = new {
      def apply(returnUrl: String = "", returnLinkText: String = "") = WS.url(resource(s"$url?returnUrl=${encryptAndEncode(returnUrl)}&returnLinkText=${encryptAndEncode(returnLinkText)}"))
    }

    val `/paperless/resend-verification-email` = urlWithHostContext("/paperless/resend-verification-email")
    val `/paperless/manage` = urlWithHostContext("/paperless/manage")

    val payeFormTypeBody = Json.parse(s"""{"active":true}""")

    def `/preferences`(header: (String, String)) = new {
      def getPreference = WS.url(server.externalResource("entity-resolver", "/preferences"))
        .withHeaders(header)
        .get

      def postPendingEmail(email: String) = WS.url(server.externalResource("entity-resolver", "/preferences/pending-email"))
        .withHeaders(header)
        .put(Json.parse(s"""{"email":"$email"}"""))
    }

    val `/portal/preferences` = new {

      def getForUtr(utr: String) = WS.url(server.externalResource("entity-resolver", s"/portal/preferences/sa/$utr")).get()

      def getForNino(nino: String) = WS.url(server.externalResource("entity-resolver", s"/portal/preferences/paye/$nino")).get()
    }

    def `/preferences/terms-and-conditions`(header: (String, String)) = new {
      def postPendingEmail(pendingEmail: String) = WS.url(server.externalResource("entity-resolver",
        s"/preferences/terms-and-conditions")).withHeaders(header).post(Json.parse(s"""{"generic":{"accepted":true}, "email":"$pendingEmail"}"""))

      def postOptOut = WS.url(server.externalResource("entity-resolver",
        s"/preferences/terms-and-conditions")).withHeaders(header).post(Json.parse("""{"generic":{"accepted":false}}"""))
    }

    def `/entity-resolver-admin/sa/:utr`(utr: String, create: Boolean = false) = {
      val response = WS.url(server.externalResource("entity-resolver", path = s"/entity-resolver-admin/sa/$utr")).get().futureValue
      if (create) response.status should be (201) else response.status should be (200)
      response.body
    }

    def `/entity-resolver-admin/paye/:nino`(nino: String, create: Boolean = false) = {
      val response = WS.url(server.externalResource("entity-resolver", path = s"/entity-resolver-admin/paye/$nino")).get().futureValue
      if (create) response.status should be (201) else response.status should be (200)
      response.body
    }


    val `/preferences-admin/sa/individual` = new {
      def verifyEmailFor(entityId: String) = WS.url(server.externalResource("preferences",
        s"/preferences-admin/$entityId/verify-email")).post(EmptyContent())

      def postExpireVerificationLink(entityId:String) = WS.url(server.externalResource("preferences",
        s"/preferences-admin/$entityId/expire-email-verification-link")).post(EmptyContent())

      def postLegacyOptOut(entityId: String) = {
        WS.url(server.externalResource("preferences", path = s"/preferences-admin/$entityId/legacy-opt-out"))
          .post(Json.parse("{}"))
      }

      def postLegacyOptIn(entityId: String, email: String) = {
        WS.url(server.externalResource("preferences", path = s"/preferences-admin/${entityId}/legacy-opt-in/${email}"))
          .post(Json.parse("{}"))
      }
    }

    val `/preferences-admin/bounce-email` = new {
      def post(emailAddress: String) = WS.url(server.externalResource("preferences",
        "/preferences-admin/bounce-email")).post(Json.parse( s"""{
             |"emailAddress": "$emailAddress"
             |}""".stripMargin))
    }

    val `/preferences-admin/sa/bounce-email-inbox-full` = new {
      def post(emailAddress: String) = WS.url(server.externalResource("preferences",
        "/preferences-admin/bounce-email")).post(Json.parse( s"""{
             |"emailAddress": "$emailAddress",
             |"code": 552
             |}""".stripMargin))
    }

    def `/paperless/warnings` = urlWithHostContext("/paperless/warnings")()
  }

  trait TestCaseWithFrontEndAuthentication extends TestCase with FrontendCookieHelper {

    import play.api.Play.current

    implicit val hc = HeaderCarrier()

    def authResource(path: String) = server.externalResource("auth", path)

    lazy val ggAuthHeaderWithUtr = createGGAuthorisationHeaderWithUtr(SaUtr(utr))
    lazy val ggAuthHeaderWithUtrAndNino = createGGAuthorisationHeaderWithUtr(SaUtr(utr), nino)
    lazy val ggAuthHeaderWithNino = createGGAuthorisationHeaderWithNino(nino)

    private lazy val ggAuthorisationHeaderWithUtr = AuthorisationProvider.forGovernmentGateway(authResource, s"utr-$utr")
    private lazy val ggAuthorisationHeaderWithNino = AuthorisationProvider.forGovernmentGateway(authResource, s"nino-${nino.value}")
    private lazy val ggAuthorisationHeaderWithUtrAndNino = AuthorisationProvider.forGovernmentGateway(authResource, s"utr-$utr--nino-${nino.value}")

    private lazy val verifyAuthorisationHeader = AuthorisationProvider.forVerify(authResource)

    def createGGAuthorisationHeaderWithUtr(ids: TaxIdentifier*): (String, String) = ggAuthorisationHeaderWithUtr.create(ids.toList).futureValue
    def createGGAuthorisationHeaderWithNino(ids: TaxIdentifier*): (String, String) = ggAuthorisationHeaderWithNino.create(ids.toList).futureValue
    def createGGAuthorisationHeaderWithUtrAndNino(ids: TaxIdentifier*): (String, String) = ggAuthorisationHeaderWithUtrAndNino.create(ids.toList).futureValue
    def createVerifyAuthorisationHeader(utr: TaxIdentifier): (String, String) = verifyAuthorisationHeader.create(utr).futureValue

    lazy val cookieWithUtr = cookieFor(ggAuthorisationHeaderWithUtr.createBearerToken(List(SaUtr(utr))).futureValue).futureValue
    lazy val cookieWithUtrAndNino = cookieFor(ggAuthorisationHeaderWithUtrAndNino.createBearerToken(List(SaUtr(utr), nino)).futureValue).futureValue
    lazy val cookieWithNino = cookieFor(ggAuthorisationHeaderWithUtr.createBearerToken(List(nino)).futureValue).futureValue

    def cookieForUtr(utr: SaUtr) = cookieFor(ggAuthorisationHeaderWithUtr.createBearerToken(List(utr)).futureValue)
    def cookieForUtrAndNino(utr: SaUtr, nino: Nino) = cookieFor(ggAuthorisationHeaderWithUtr.createBearerToken(List(utr, nino)).futureValue)
    def cookieForTaxIdentifiers(taxIdentifiers: TaxIdentifier*) = cookieFor(ggAuthorisationHeaderWithUtr.createBearerToken(taxIdentifiers.toList).futureValue).futureValue


    val returnUrl = "/test/return/url"
    val returnLinkText = "Continue"

    val encryptedReturnUrl = URLEncoder.encode(QueryParameterCrypto.encrypt(PlainText(returnUrl)).value, "UTF-8")
    val encryptedReturnText = URLEncoder.encode(QueryParameterCrypto.encrypt(PlainText(returnLinkText)).value, "UTF-8")

    def `/paperless/activate`(taxIdentifier: TaxIdentifier)(additionalUserTaxIdentifiers: TaxIdentifier*) = new {

      private val url = WS.url(resource("/paperless/activate"))
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

