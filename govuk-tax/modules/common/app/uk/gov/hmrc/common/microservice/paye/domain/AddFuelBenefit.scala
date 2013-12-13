package uk.gov.hmrc.common.microservice.paye.domain

import org.joda.time.LocalDate

case class AddFuelBenefitConfirmationData(employerName: Option[String],
                                          providedFrom: Option[LocalDate],
                                          employerPayFuel: String,
                                          dateFuelWithdrawn: Option[LocalDate])
