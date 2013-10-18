package uk.gov.hmrc.common.microservice.paye.domain

import org.joda.time.LocalDate


case class RemoveBenefitCalculationResponse(result: Map[String, BigDecimal])

case class NewBenefitCalculationData(registeredBefore98: Boolean,
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

case class NewBenefitCalculationResponse(carBenefitValue:Option[Int], fuelBenefitValue:Option[Int])


