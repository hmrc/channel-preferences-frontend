import java.util.UUID

import org.jsoup.Jsoup
import org.scalatest.concurrent.ScalaFutures
import play.api.Play.current
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.libs.ws.{WS, WSResponse}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.connectors.HeaderCarrier
import uk.gov.hmrc.play.it.{BearerTokenHelper, ExternalService, MicroServiceEmbeddedServer, ServiceSpec}
import uk.gov.hmrc.play.test.ResponseMatchers

import scala.concurrent.Future

class AccountDetailPartialISpec extends ServiceSpec with TestUser {

  "Account detail partial" should {
    "return not authorised when no credentials supplied" in new TestCase {
      `/email-reminders-status`.get should have(status(401))
    }

    "return not authorised when not enrolled in SA" in new TestCase {
      `/email-reminders-status`.withHeaders(authenticationCookie(userId = "643212300020", password)).get should have (status(401))
    }

    "return opted out details when no preference is set" in new TestCase {
      val response = `/email-reminders-status`.withHeaders(authenticationCookie(userId, password)).get
      response should have(status(200))
      response.futureValue.body should (
          include("Self Assessment email reminders") and
          not include ("You need to verify")
        )
    }

    "contain pending details when a pending email is present" in new TestCase {
      val email = s"${UUID.randomUUID().toString}@email.com"
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))

      val response = `/email-reminders-status`.withHeaders(authenticationCookie(userId, password)).get
      response should have(status(200))
      response.futureValue.body should (
        include(s"You need to verify")
      )
    }
  }

  trait TestCase extends ScalaFutures with ResponseMatchers with BearerTokenHelper {
    implicit val hc = HeaderCarrier()

    def authResource(path: String) = server.externalResource("auth", path)

    def authToken = AuthorisationHeader(Some(getBarerToken(SaUtr(utr))))

    def `/email-reminders-status` = WS.url(resource("/account/account-details/sa/email-reminders-status"))

    val `/preferences-admin/sa/individual/print-suppression` = new {
      def deleteAll() = WS.url(server.externalResource("preferences", "/preferences-admin/sa/individual/print-suppression")).delete()
    }

    val `/portal/preferences/sa/individual` = new {
      def postPendingEmail(utr: String, pendingEmail: String) = WS.url(server.externalResource("preferences",
        s"/portal/preferences/sa/individual/$utr/print-suppression")).post(Json.parse( s"""{"digital": true, "email":"$pendingEmail"}"""))
    }

    def authenticationCookie(userId: String, password: String) = {
      def cookieFrom(response: Future[WSResponse]) = {
        HeaderNames.COOKIE -> response.futureValue.header(HeaderNames.SET_COOKIE).get
      }

      def csrfTokenAndAuthenticateUrlFrom(accountSignInResponse : Future[WSResponse]): (String, String) = {
        val form = Jsoup.parse(accountSignInResponse.futureValue.body).getElementsByTag("form").first
        val csrfToken: String = form.getElementsByAttributeValue("name", "csrfToken").first.attr("value")
        csrfToken should not be empty
        (form.attr("action"), csrfToken)
      }

      val accountSignInResponse = `/account/sign-in`
      accountSignInResponse should have(status(200))

      val (authenticateUrl: String, csrfToken: String) = csrfTokenAndAuthenticateUrlFrom(accountSignInResponse)

      val loginResponse = WS.url(server.externalResource("ca-frontend", authenticateUrl)).withHeaders(cookieFrom(accountSignInResponse)).post(Map("csrfToken" -> csrfToken, "userId" -> userId, "password" -> password).mapValues(Seq(_)))

      cookieFrom(loginResponse)
    }

    def `/account/sign-in` = WS.url(server.externalResource("ca-frontend", "/account/sign-in")).get()

    case class AuthorisationHeader(value: Option[String]) {
      def asHeader: Seq[(String, String)] = value.fold(Seq.empty[(String, String)])(v => Seq(HeaderNames.AUTHORIZATION -> v))
    }

    `/preferences-admin/sa/individual/print-suppression`.deleteAll should have(status(200))
  }

  override protected val server = new PreferencesFrontendIntegrationServer("AccountDetailPartialISpec")

  class PreferencesFrontendIntegrationServer(override val testName: String) extends MicroServiceEmbeddedServer {
    override protected val externalServices: Seq[ExternalService] = Seq(
      "datastream",
      "external-government-gateway",
      "government-gateway",
      "ca-frontend",
      "preferences",
      "email",
      "auth").map(ExternalService.runFromJar(_))
  }
}

trait TestUser {
  def userId = "SA0055"
  val password = "testing123"
  def utr = "1555369043"
}
