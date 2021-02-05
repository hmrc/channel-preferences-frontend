/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.channelpreferencesfrontend.config

import javax.inject.{ Inject, Singleton }

import play.api.i18n.MessagesApi
import play.api.mvc.Request
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import uk.gov.hmrc.channelpreferencesfrontend.views.html.ErrorTemplate

@Singleton
class ErrorHandler @Inject() (errorTemplate: ErrorTemplate, val messagesApi: MessagesApi)(implicit appConfig: AppConfig)
    extends FrontendErrorHandler {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit
    request: Request[_]
  ): Html =
    errorTemplate(pageTitle, heading, message)
}
