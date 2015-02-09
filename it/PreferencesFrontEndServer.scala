import java.util.UUID

import org.jsoup.Jsoup
import play.api.Play.current
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.libs.ws.{WS, WSResponse}
import play.api.mvc.Results.EmptyContent
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.http.test.ResponseMatchers
import uk.gov.hmrc.play.it.{ExternalService, MicroServiceEmbeddedServer, ServiceSpec}
import uk.gov.hmrc.test.it.BearerTokenHelper
import uk.gov.hmrc.time.DateTimeUtils
import views.sa.prefs.helpers.DateFormat

import scala.concurrent.Future

trait TestUser {
  def userId = "SA0055"

  val password = "testing123"

  def utr = "1555369043"
}

trait UserAuthentication extends BearerTokenHelper with PreferencesFrontEndServer with ResponseMatchers with TestUser {

  implicit val hc = HeaderCarrier()

  def authResource(path: String) = server.externalResource("auth", path)

  def authToken = AuthorisationHeader(Some(createBearerTokenFor(SaUtr(utr))))

  def authenticationCookie(userId: String, password: String) = {
    def cookieFrom(response: Future[WSResponse]) = {
      HeaderNames.COOKIE -> response.futureValue.header(HeaderNames.SET_COOKIE).getOrElse(throw new IllegalStateException("Failed to set auth cookie"))
    }

    def csrfTokenAndAuthenticateUrlFrom(accountSignInResponse: Future[WSResponse]): (String, String) = {
      val form = Jsoup.parse(accountSignInResponse.futureValue.body).getElementsByTag("form").first
      val csrfToken: String = form.getElementsByAttributeValue("name", "csrfToken").first.attr("value")
      csrfToken should not be empty
      (form.attr("action"), csrfToken)
    }

    val accountSignInResponse = `/account/sign-in`
    accountSignInResponse should have(status(200))

    val (authenticateUrl: String, csrfToken: String) = csrfTokenAndAuthenticateUrlFrom(accountSignInResponse)

    val loginResponse = WS.url(server.externalResource("ca-frontend", authenticateUrl))
      .withHeaders(cookieFrom(accountSignInResponse))
      .post(Map("csrfToken" -> csrfToken, "userId" -> userId, "password" -> password).mapValues(Seq(_)))

    cookieFrom(loginResponse)
  }

  def `/account/sign-in` = WS.url(server.externalResource("ca-frontend", "/account/sign-in")).get()

  case class AuthorisationHeader(value: Option[String]) {
    def asHeader: Seq[(String, String)] = value.fold(Seq.empty[(String, String)])(v => Seq(HeaderNames.AUTHORIZATION -> v))
  }

}

trait PreferencesFrontEndServer extends ServiceSpec {
  protected val server = new PreferencesFrontendIntegrationServer("AccountDetailPartialISpec")

  class PreferencesFrontendIntegrationServer(override val testName: String) extends MicroServiceEmbeddedServer {
    override protected val externalServices: Seq[ExternalService] = Seq(
      "external-government-gateway",
      "government-gateway",
      "ca-frontend",
      "preferences",
      "message",
      "mailgun",
      "email",
      "auth",
      "datastream").map(ExternalService.runFromJar(_))
  }

  class TestCase extends TestUser {

    val todayDate = DateFormat.longDateFormat(Some(DateTimeUtils.now.toLocalDate)).get.body

    def uniqueEmail = s"${UUID.randomUUID().toString}@email.com"

    def changedUniqueEmail = s"${UUID.randomUUID().toString}@email.com"

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

}

