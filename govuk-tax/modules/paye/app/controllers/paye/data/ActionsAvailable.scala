package controllers.paye.data

import uk.gov.hmrc.common.microservice.paye.domain.CarBenefit

class ActionsAvailable(activeCarBenefit: Option[CarBenefit], currentTaxYear: Int, currentEmploymentSequenceNumber: Int) {

  val taxYear = activeCarBenefit.map(_.taxYear).getOrElse(currentTaxYear)
  val employmentSequenceNumber = activeCarBenefit.map(_.employmentSequenceNumber).getOrElse(currentEmploymentSequenceNumber)

  def canReplaceCar = activeCarBenefit.isDefined
  def canRemoveCar =  activeCarBenefit.isDefined
  def canRemoveFuel = activeCarBenefit.exists(_.hasActiveFuel)
  def canAddFuel =    activeCarBenefit.isDefined &&
                      !activeCarBenefit.exists(car => car.hasActiveFuel || car.fuelType == "electricity")
  def canAddCar =     !canRemoveCar
}
