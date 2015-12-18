import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.HeaderNames
import play.api.mvc.{Session, Cookie, Cookies}
import uk.gov.hmrc.crypto.{PlainText, ApplicationCrypto}
import uk.gov.hmrc.endtoend.sa.{ToAbsoluteUrl, Page}
import uk.gov.hmrc.endtoend.sa.config.UserWithUtr
import uk.gov.hmrc.test.it.BearerToken


object Stubs {
  object Preferences {
    def `GET /preferences/sa/individual/<utr>/print-suppression`(implicit user: UserWithUtr) =
      get(urlEqualTo(s"/preferences/sa/individual/${user.utr}/print-suppression"))

    val optedInPreferenceJson =
      s"""
         |{
         |    "digital": true
         |}""".stripMargin

    def `POST /preferences/sa/individual/<utr>/terms-and-conditions`(genericAccepted: Boolean)(implicit user: UserWithUtr) =
      postRequestedFor(urlEqualTo(s"/preferences/sa/individual/${user.utr}/terms-and-conditions")).withRequestBody(
        equalToJson(s"""{ "generic": { "accepted": $genericAccepted } } """)
      )
  }

  object Auth {
    def loginPage(implicit user: UserWithUtr) = StubbedPage(
      title = "Login",
      relativeUrl = "login",
      name = "Auth.LoginPage",
      responseBody = "",
      responseHeader = HeaderNames.SET_COOKIE -> Cookies.encode(Seq(cookieFor(BearerToken(user.utr), userId = s"/auth/oid/${user.utr}")))
    )

    private def cookieFor(bearerToken: BearerToken, authProvider: String = "GGW", userId: String): Cookie = {
      val keyValues = Map(
        "authToken" -> bearerToken.token,
        "token" -> "system-assumes-valid-token",
        "userId" -> userId,
        "ap" -> authProvider
      )
      Cookie(name = "mdtp", value = ApplicationCrypto.SessionCookieCrypto.encrypt(PlainText(Session.encode(keyValues))).value)
    }

    val `GET /auth/authority` = get(urlEqualTo("/auth/authority"))
    def authorityRecordJson(implicit user: UserWithUtr) =
      s"""{
             "uri": "/auth/oid/${user.utr}",
             "loggedInAt": "2014-06-09T14:57:09.522Z",
             "accounts": {
                "sa": {
                 "link": "/sa/individual/${user.utr}",
                 "utr": "${user.utr}"
                }
             },
             "levelOfAssurance": "2",
             "confidenceLevel": 50
         }"""
  }

  object Host {
    def ReturnPage = StubbedPage(
      title          = "Redirected Page",
      relativeUrl    = "some/other/page",
      name           = "HostStub.pageToReturnTo",
      responseBody   = "<html><head><title>Redirected Page</title></head></html>"
    )
  }

  case class StubbedPage(title: String,
                         relativeUrl: String,
                         name: String,
                         responseBody: String,
                         responseHeader: (String, String)*) extends Page {
    stubFor(get(urlEqualTo(s"/$relativeUrl")).willReturn {
      val resp = aResponse withStatus 200 withBody responseBody
      responseHeader.foldLeft(resp) { (r, h) => r.withHeader(h._1, h._2) }
    })

    override def toString() = name
  }

  implicit def stubbedUrls[T <: Stubs.StubbedPage]: ToAbsoluteUrl[T] =
    ToAbsoluteUrl.fromRelativeUrl[T](host = "localhost", port = 8080)
}
