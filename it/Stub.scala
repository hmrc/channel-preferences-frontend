import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.HeaderNames
import play.api.mvc.{Cookie, Cookies}
import uk.gov.hmrc.endtoend.sa.Page


object Stub {
  object Preferences {
    def `GET /preferences/sa/individual/<utr>/print-suppression`(utr: String) = get(urlEqualTo(s"/preferences/sa/individual/$utr/print-suppression"))
    val optedInPreferenceJson =
      s"""
         |{
         |    "digital": true
         |}""".stripMargin

    def `POST /preferences/sa/individual/<utr>/terms-and-conditions`(utr: String)(genericAccepted: Boolean) =
      postRequestedFor(urlEqualTo(s"/preferences/sa/individual/$utr/terms-and-conditions")).withRequestBody(
        equalToJson(s"""{ "generic": { "accepted": $genericAccepted } } """)
      )
  }

  object Auth {
    def loginPage(cookieToSet: Cookie) = StubbedPage(
      title = "Login",
      relativeUrl = "login",
      name = "Auth.LoginPage",
      responseBody = "",
      responseHeader = HeaderNames.SET_COOKIE -> Cookies.encode(Seq(cookieToSet))
    )

    val `GET /auth/authority` = get(urlEqualTo("/auth/authority"))
    val authorityRecordJson =
      """{
             "uri": "/auth/oid/1234567890",
             "loggedInAt": "2014-06-09T14:57:09.522Z",
             "accounts": {
                "sa": {
                 "link": "/sa/individual/1111111111",
                 "utr": "1111111111"
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
                         responseHeader: (String, String)*) extends Page with Stubbed {
    stubFor(get(urlEqualTo(s"/$relativeUrl")).willReturn {
      val resp = aResponse withStatus 200 withBody responseBody
      responseHeader.foldLeft(resp) { (r, h) => r.withHeader(h._1, h._2) }
    })

    override def toString() = name
  }
}
