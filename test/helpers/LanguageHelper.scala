/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package helpers

import controllers.auth.AuthenticatedRequest
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.HeaderNames
import play.api.i18n.{ Lang, Messages, MessagesApi, MessagesImpl }
import play.api.test.FakeRequest

trait LanguageHelper {
  this: GuiceOneAppPerSuite =>
  implicit val messageApi = app.injector.instanceOf[MessagesApi]
  val langCy = Lang("cy")
  val langEn = Lang("en")
  val fakeRequest = FakeRequest("GET", "/")
  val headers = fakeRequest.headers.add((HeaderNames.ACCEPT_LANGUAGE, ("cy")))
  val welshRequest = AuthenticatedRequest(fakeRequest.withHeaders(headers), None, None, None, None)
  val headersEn = fakeRequest.headers.add((HeaderNames.ACCEPT_LANGUAGE, ("en")))
  val engRequest = AuthenticatedRequest(fakeRequest.withHeaders(headers), None, None, None, None)

  def messagesInWelsh(): Messages = MessagesImpl(langCy, messageApi)
  def messagesInEnglish(): Messages = MessagesImpl(langEn, messageApi)
}
