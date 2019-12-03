/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
