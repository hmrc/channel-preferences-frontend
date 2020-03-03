/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers.internal

import connectors.{ GenericTerms, TaxCreditsTerms, TermsType }
import play.api.mvc.PathBindable
import uk.gov.hmrc.abtest.Cohort

sealed trait OptInCohort extends Cohort {
  val id: Int
  val name: String
  val terms: TermsType

  override def toString: String = name
}

object OptInCohort {

  def fromId(id: Int): Option[OptInCohort] = OptInCohortConfigurationValues.cohorts.values.find(c => c.id == id)

  implicit val pathBinder: PathBindable[Option[OptInCohort]] = PathBindable.bindableInt.transform(
    fromId,
    _.map(_.id).getOrElse(throw new IllegalArgumentException("Cannot generate a URL for an unknown Cohort")))
}

object IPage extends OptInCohort {
  override val id: Int = 8
  override val name: String = "IPage"
  override val terms: TermsType = GenericTerms
}

object TCPage extends OptInCohort {
  override val id: Int = 9
  override val name: String = "TCPage"
  override val terms: TermsType = TaxCreditsTerms
}
