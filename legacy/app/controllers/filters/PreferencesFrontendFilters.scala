/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers.filters

import play.api.http.HttpFilters
import play.api.mvc.EssentialFilter
import uk.gov.hmrc.play.bootstrap.filters.FrontendFilters

import javax.inject.{ Inject, Singleton }

@Singleton
class PreferencesFrontendFilters @Inject() (
  frontendFilters: FrontendFilters,
  exceptionHandlingFilter: ExceptionHandlingFilter
) extends HttpFilters {

  override val filters: Seq[EssentialFilter] = frontendFilters.filters :+ exceptionHandlingFilter
}
