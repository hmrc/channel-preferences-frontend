import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.HeaderNames
import play.api.mvc.{Cookies, Cookie}


trait Stubs {
  object Preferences {
    def stubPreference(utr: String) = stubFor(get(urlEqualTo(s"/preferences/sa/individual/$utr/print-suppression"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(
            s"""
               |{
               |    "digital": true
               |}
              """.stripMargin
          )))

    def verifyPostTermsAndCondition(utr: String, genericAccepted: Boolean ) =
      verify(postRequestedFor(urlEqualTo(s"/preferences/sa/individual/$utr/terms-and-conditions"))
        .withRequestBody(equalToJson(
          s"""{ "generic": {
              |  "accepted": $genericAccepted
              |  }
              |}
         """.stripMargin)))
  }

  object Auth {
    def stubLoginPage(cookie: Cookie) =
      stubFor(get(urlEqualTo("/login"))
        .willReturn(
          aResponse()
            .withHeader(HeaderNames.SET_COOKIE, Cookies.encode(Seq(cookie)))
            .withStatus(200)
        ))

    def stubAuth() =
      stubFor(get(urlEqualTo("/auth/authority"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(
              s"""
                 |{
                 |    "uri": "/auth/oid/1234567890",
                 |    "loggedInAt": "2014-06-09T14:57:09.522Z",
                 |    "accounts": {
                 |       "sa": {
                 |        "link": "/sa/individual/1111111111",
                 |        "utr": "1111111111"
                 |       }
                 |    },
                 |    "levelOfAssurance": "2",
                 |    "confidenceLevel": 50
                 |}
              """.stripMargin
            )))
   }

  object Redirect {
    def stubRedirectPage = stubFor(get(urlEqualTo(s"/some/other/page"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withHeader("X-Title", "Redirected Page")
          .withBody(
            "<html><head><title>Redirected Page</title></head></html>"
          )))
  }
}
