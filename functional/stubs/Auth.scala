package stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.HeaderNames
import play.api.mvc.{Cookie, Cookies, Session}
import stubs.Page.StubbedPage
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.endtoend.sa.config.UserWithUtr

object Auth {

  def loginPage(implicit user: UserWithUtr) = StubbedPage(
    title = "Login",
    relativeUrl = "login",
    name = "Auth.LoginPage",
    responseBody = ""
//    responseHeader = HeaderNames.SET_COOKIE -> Cookies.encodeCookieHeader(Seq(cookieFor(BearerToken(user.utr), userId = s"/auth/oid/${user.utr}")))
  )

//  private def cookieFor(bearerToken: BearerToken, authProvider: String = "GGW", userId: String): Cookie = {
//    val keyValues = Map(
//      "authToken" -> bearerToken.token,
//      "token" -> "system-assumes-valid-token",
//      "userId" -> userId,
//      "ap" -> authProvider
//    )
//    Cookie(name = "mdtp", value = ApplicationCrypto.SessionCookieCrypto.encrypt(PlainText(Session.encode(keyValues))).value)
//  }

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
             "confidenceLevel": 50,
             "credentialStrength": "weak",
             "legacyOid": ""
         }"""

}
