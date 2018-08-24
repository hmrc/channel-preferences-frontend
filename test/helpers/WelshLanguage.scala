package helpers

import controllers.auth.AuthenticatedRequest
import play.api.http.HeaderNames
import play.api.i18n.{Lang, Messages}
import play.api.test.FakeRequest

trait WelshLanguage {
  val langCy = new Lang("cy")
  val langEn = new Lang("en")
  val fakeRequest = FakeRequest("GET", "/")
  val headers = fakeRequest.headers.add((HeaderNames.ACCEPT_LANGUAGE, ("cy")))
  val welshRequest = AuthenticatedRequest(fakeRequest.copyFakeRequest(headers = headers), None, None, None, None)

  def messagesInWelsh(applicationMessages: Messages): Messages = applicationMessages.copy(lang = langCy)
}
