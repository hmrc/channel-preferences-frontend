/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers.filters

import javax.inject.{ Inject, Singleton }
import play.api.http.HttpFilters
import play.api.mvc.EssentialFilter
import uk.gov.hmrc.play.bootstrap.filters.FrontendFilters

@Singleton
class PreferencesFrontendFilters @Inject()(
  frontendFilters: FrontendFilters,
  exceptionHandlingFilter: ExceptionHandlingFilter)
    extends HttpFilters {

  override val filters: Seq[EssentialFilter] = frontendFilters.filters :+ exceptionHandlingFilter
}
