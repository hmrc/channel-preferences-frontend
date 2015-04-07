import java.util.UUID

import play.api.Play.current
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.libs.ws.WS
import play.api.mvc.Results.EmptyContent
import play.api.mvc.{Cookie, Cookies, Session}
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.it.{ExternalService, MicroServiceEmbeddedServer, ServiceSpec}
import uk.gov.hmrc.test.it.BearerTokenHelper
import uk.gov.hmrc.time.DateTimeUtils
import views.sa.prefs.helpers.DateFormat

trait TestUser {
  def userId = "SA0055"

  val password = "testing123"

  def utr = "1555369043"
}

trait PreferencesFrontEndServer extends ServiceSpec {
  protected val server = new PreferencesFrontendIntegrationServer("AccountDetailPartialISpec")

  class PreferencesFrontendIntegrationServer(override val testName: String) extends MicroServiceEmbeddedServer {
    override protected val externalServices: Seq[ExternalService] = Seq(
      "external-government-gateway",
      "government-gateway",
      "auth",
      "preferences",
      "message",
      "mailgun",
      "ca-frontend",
      "email",
      "datastream").map(ExternalService.runFromJar(_))
  }

  class TestCase extends TestUser {

    val todayDate = DateFormat.longDateFormat(Some(DateTimeUtils.now.toLocalDate)).get.body

    def uniqueEmail = s"${UUID.randomUUID().toString}@email.com"

    def changedUniqueEmail = uniqueEmail

    def `/email-reminders-status` = WS.url(resource("/account/account-details/sa/email-reminders-status"))

    val `/portal/preferences/sa/individual` = new {
      def postPendingEmail(utr: String, pendingEmail: String) = WS.url(server.externalResource("preferences",
        s"/portal/preferences/sa/individual/$utr/print-suppression")).post(Json.parse( s"""{"digital": true, "email":"$pendingEmail"}"""))

      def postDeEnrolling(utr: String) = WS.url(server.externalResource("preferences",
        s"/portal/preferences/sa/individual/$utr/print-suppression")).post(Json.parse(s"""{"de-enrolling": true, "reason": "Pour le-test"}"""))

      def postOptOut(utr: String) = WS.url(server.externalResource("preferences",
        s"/portal/preferences/sa/individual/$utr/print-suppression")).post(Json.parse( s"""{"digital": false}"""))

      def getPreferences(utr: String) = WS.url(server.externalResource("preferences", s"/portal/preferences/sa/individual/$utr/print-suppression")).get()
    }

    val `/preferences-admin/sa/individual` = new {
      def verifyEmailFor(utr: String) = WS.url(server.externalResource("preferences",
        s"/preferences-admin/sa/individual/$utr/verify-email")).post(EmptyContent())

      def postExpireVerificationLink(utr:String) = WS.url(server.externalResource("preferences",
        s"/preferences-admin/sa/individual/$utr/expire-email-verification-link")).post(EmptyContent())

      def delete(utr: String) = WS.url(server.externalResource("preferences",
        s"/preferences-admin/sa/individual/$utr/print-suppression")).delete()

      def deleteAll() = WS.url(server.externalResource("preferences",
        "/preferences-admin/sa/individual/print-suppression")).delete()
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

    def `/account/preferences/warnings` = {
      WS.url(resource("/account/preferences/warnings"))
    }
  }

  trait TestCaseWithFrontEndAuthentication extends TestCase with BearerTokenHelper {

    implicit val hc = HeaderCarrier()

    def authResource(path: String) = {
      server.externalResource("auth", path)
    }

    lazy val keyValues = Map(
      "authToken" -> createBearerTokenFor(SaUtr(utr)).futureValue,
      "token" -> "system-assumes-valid-token",
      "userId" -> "/auth/oid/system-assumes-valid-oid"
    )

    lazy val cookie = HeaderNames.COOKIE ->
      Cookies.encode(Seq(Cookie("mdtp", ApplicationCrypto.SessionCookieCryptoDeprecated.encrypt(PlainText(Session.encode(keyValues))).value)))
  }

}

