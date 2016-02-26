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
import uk.gov.hmrc.test.it.{AuthorisationHeader, FrontendCookieHelper}
import uk.gov.hmrc.time.DateTimeUtils
import views.sa.prefs.helpers.DateFormat

import scala.concurrent.duration._

trait TestUser {
  def userId = "SA0055"

  val password = "testing123"

  val utr = GenerateRandom.utr().value
}

trait PreferencesFrontEndServer extends ServiceSpec {
  protected val server = new PreferencesFrontendIntegrationServer("PreferencesFrontEndServer")

  class PreferencesFrontendIntegrationServer(override val testName: String) extends MicroServiceEmbeddedServer {
    override protected val externalServices: Seq[ExternalService] = externalServiceNames.map(ExternalService.runFromJar(_))

    override protected def startTimeout: Duration = 300.seconds
  }

  def externalServiceNames: Seq[String] = {
    Seq(
      "external-government-gateway",
      "government-gateway",
      "auth",
      "message",
      "mailgun",
      "hmrc-deskpro",
      "ca-frontend",
      "email",
      "cid",
      "entity-resolver",
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

    def `/preferences/sa/individual/utr/print-suppression`(header: (String, String)) = new {
      def getPreference(utr: String) = WS.url(server.externalResource("entity-resolver",
        s"/preferences/sa/individual/${utr}/print-suppression"))
        .withHeaders(header)
        .get
    }

    val `/portal/preferences/sa/individual` = new {

      def postDeEnrolling(utr: String) = WS.url(server.externalResource("entity-resolver",
        s"/portal/preferences/sa/individual/$utr/print-suppression")).post(Json.parse(s"""{"de-enrolling": true, "reason": "Pour le-test"}"""))

      def get(utr: String) = WS.url(server.externalResource("entity-resolver", s"/portal/preferences/sa/individual/$utr/print-suppression")).get()
      }

    def `/preferences/sa/individual/utr/terms-and-conditions`(header: (String, String)) = new {
      def postPendingEmail(utr: String, pendingEmail: String) = WS.url(server.externalResource("entity-resolver",
        s"/preferences/sa/individual/$utr/terms-and-conditions")).withHeaders(header).post(Json.parse(s"""{"generic":{"accepted":true}, "email":"$pendingEmail"}"""))

      def postOptOut(utr: String) = WS.url(server.externalResource("entity-resolver",
        s"/preferences/sa/individual/$utr/terms-and-conditions")).withHeaders(header).post(Json.parse("""{"generic":{"accepted":false}}"""))
    }

    val `/preferences-admin/sa/individual` = new {
      def verifyEmailFor(utr: String) = WS.url(server.externalResource("preferences",
        s"/preferences-admin/sa/individual/$utr/verify-email")).post(EmptyContent())

      def postExpireVerificationLink(utr:String) = WS.url(server.externalResource("preferences",
        s"/preferences-admin/sa/individual/$utr/expire-email-verification-link")).post(EmptyContent())

      def postLegacyOptOut(utr: String) = {
        WS.url(server.externalResource("preferences", path = s"/preferences-admin/sa/individual/$utr/legacy-opt-out"))
          .post(Json.parse("{}"))
      }

      def postLegacyOptIn(utr: String, email: String) = {
        WS.url(server.externalResource("preferences", path = s"/preferences-admin/sa/individual/${utr}/legacy-opt-in/${email}"))
          .post(Json.parse("{}"))
      }
    }

    val `/preferences-admin/sa/bounce-email` = new {
      def post(emailAddress: String) = WS.url(server.externalResource("preferences",
        "/preferences-admin/sa/bounce-email")).post(Json.parse( s"""{
             |"emailAddress": "$emailAddress"
             |}""".stripMargin))
    }

    val `/preferences-admin/sa/bounce-email-inbox-full` = new {
      def post(emailAddress: String) = WS.url(server.externalResource("preferences",
        "/preferences-admin/sa/bounce-email")).post(Json.parse( s"""{
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

    lazy val ggAuthHeader = createGGAuthorisationHeader(SaUtr(utr))

    private lazy val ggAuthorisationHeader = AuthorisationHeader.forGovernmentGateway(authResource, s"utr-${utr}")
    private lazy val verifyAuthorisationHeader = AuthorisationHeader.forVerify(authResource)

    def createGGAuthorisationHeader(ids: TaxIdentifier*): (String, String) = ggAuthorisationHeader.create(ids.toList).futureValue
    def createVerifyAuthorisationHeader(utr: TaxIdentifier): (String, String) = verifyAuthorisationHeader.create(utr).futureValue

    lazy val cookie = cookieFor(ggAuthorisationHeader.createBearerToken(List(SaUtr(utr))).futureValue).futureValue

    def cookieForUtr(utr: SaUtr) = cookieFor(ggAuthorisationHeader.createBearerToken(List(utr)).futureValue)
    def cookieForUtrAndNino(utr: SaUtr, nino: Nino) = cookieFor(ggAuthorisationHeader.createBearerToken(List(utr, nino)).futureValue)
    def cookieForTaxIdentifiers(taxIdentifiers: TaxIdentifier*) = cookieFor(ggAuthorisationHeader.createBearerToken(taxIdentifiers.toList).futureValue).futureValue


    val returnUrl = "/test/return/url"
    val returnLinkText = "Continue"

    val encryptedReturnUrl = URLEncoder.encode(QueryParameterCrypto.encrypt(PlainText(returnUrl)).value, "UTF-8")
    val encryptedReturnText = URLEncoder.encode(QueryParameterCrypto.encrypt(PlainText(returnLinkText)).value, "UTF-8")

    def `/paperless/activate/:form-type/:tax-identifier`(formType: String, taxIdentifier: TaxIdentifier)(additionalUserTaxIdentifiers: TaxIdentifier*) = new {

      private val url = WS.url(resource(s"/paperless/activate/$formType/${taxIdentifier.value}"))
        .withHeaders(createGGAuthorisationHeader(taxIdentifier +: additionalUserTaxIdentifiers: _*), cookieForTaxIdentifiers(taxIdentifier +: additionalUserTaxIdentifiers: _*))
        .withQueryString(
          "returnUrl" -> QueryParameterCrypto.encrypt(PlainText(returnUrl)).value,
          "returnLinkText" -> QueryParameterCrypto.encrypt(PlainText(returnLinkText)).value
        )

      private val formTypeBody = Json.parse("""{"active":true}""")

      def put() = url.put(formTypeBody)
    }
  }

}

