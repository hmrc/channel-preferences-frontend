package uk.gov.hmrc.common.microservice.paye.domain

import org.joda.time.LocalDate


case class RemoveBenefitCalculationResult(result: Map[String, BigDecimal])

case class AddBenefitCalculationData(carRegisteredBefore98: Boolean,
                                  fuelType: String,
                                  co2Emission: Option[Int],
                                  engineCapacity: Option[Int],
                                  userContributingAmount: Option[Int],
                                  listPrice: Int,
                                  carBenefitStartDate: Option[LocalDate],
                                  carBenefitStopDate: Option[LocalDate],
                                  numDaysCarUnavailable: Option[Int],
                                  employeePayments: Option[Int],
                                  employerPayFuel: String,
                                  fuelBenefitStopDate: Option[LocalDate]
                                   )

case class AddBenefitResponse(carBenefitValue:Option[Int], fuelBenefitValue:Option[Int])


