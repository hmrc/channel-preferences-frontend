package helpers

import play.api.http.HeaderNames
import play.api.i18n.Lang
import play.api.test.FakeRequest
import play.api.i18n.Messages

trait WelshLanguage {
  val langCy = new Lang("cy")
  val langEn = new Lang("en")
  val fakeRequest = FakeRequest("GET", "/")
  val headers = fakeRequest.headers.add((HeaderNames.ACCEPT_LANGUAGE, ("cy")))
  val welshRequest = fakeRequest.copyFakeRequest(headers = headers)
  def messagesInWelsh(applicationMessages : Messages) : Messages = applicationMessages.copy(lang = langCy)
}
