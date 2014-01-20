package controllers.paye

import play.mvc.Content
import org.jsoup.Jsoup
import play.api.test.Helpers._
import play.api.test.{FakeApplication, WithApplication}
import models.paye.BenefitFixture
import views.html.paye.display_car_and_fuel
import play.api.i18n.Messages
import uk.gov.hmrc.utils.DateConverter
import controllers.DateFieldsHelper
import uk.gov.hmrc.common.microservice.paye.domain.CarBenefit

class CarAndFuelTemplateSpec extends PayeBaseSpec with DateConverter with DateFieldsHelper {


  trait BaseData {
    def carBenefit = carBenefitEmployer1

    def cars: Seq[CarBenefit] = Seq(CarBenefit(carBenefit))

    def employments = johnDensmoresOneEmployment(1)

    def rawTaxData = RawTaxData(2013, cars, employments, Seq.empty, Seq.empty)
  }



  "car and fuel template" should {
    def documentOf(content: Content) = Jsoup.parse(contentAsString(content))

    "render with all car details for a company car with no fuel" in new WithApplication(FakeApplication()) with BaseData {
      // given
      val aCompanyCarWihNoFuel = BenefitFixture.carWithoutFuel
      val anEmployerName = "MyCompany"

      // when
      val doc = documentOf(display_car_and_fuel(aCompanyCarWihNoFuel, anEmployerName))

      // then
      doc.select("#company-name").text should include (anEmployerName)
      doc.select("#car-benefit-date-available").text should be (BenefitFixture.carBenefitAvailableDateString)
      doc.select("#car-benefit-date-registered").text should be (BenefitFixture.carBenefitRegisteredDateString)
      doc.select("#car-benefit-car-value").text should be (BenefitFixture.carValuePounds)
      doc.select("#car-benefit-employee-capital-contribution").text should be (BenefitFixture.carEmployeeCapitalContributionVauePounds)
      doc.select("#car-benefit-employee-payments").text should be (BenefitFixture.carEmployeePrivateUseContributionVauePounds)
      doc.select("#car-co2-emissions").text should be (BenefitFixture.carCo2Emissions + "g CO2/km")
      doc.select("#car-benefit-engine").text should be (Messages(s"paye.add_car_benefit.engine_capacity.${BenefitFixture.carEngineSize}"))
      doc.select("#car-benefit-fuel-type").text should be (Messages(s"paye.add_car_benefit.fuel_type.${BenefitFixture.carFuelType}"))
      doc.select("#no-car-benefit-container") should be (empty)
      doc.select("#private-fuel").text should be (Messages("paye.add_car_benefit.employer_pay_fuel.false"))
    }

    "render a car with fuel included for a company car with fuel" in new WithApplication(FakeApplication()) with BaseData {
      // given
      val aCompanyCarWithFuel = BenefitFixture.carWithFuel
      val anEmployerName = "MyCompany"

      // when
      val doc = documentOf(display_car_and_fuel(aCompanyCarWithFuel, anEmployerName))

      // then
      doc.select("#company-name").text should include (anEmployerName)
      doc.select("#private-fuel").text should be (Messages("paye.home.employer_pay_fuel.true"))
    }

    "not render the co2 emissions, private fuel or engine type fields if the fuel type is electric" in new WithApplication(FakeApplication()) with BaseData {
      // given
      val anElectricCar = BenefitFixture.carWithoutFuel.copy(fuelType = "electricity")
      val anEmployerName = "MyCompany"

      // when
      val doc = documentOf(display_car_and_fuel(anElectricCar, anEmployerName))

      // then
      doc.select("#company-name").text should include (anEmployerName)
      doc.select("#car-benefit-date-available").text should be (BenefitFixture.carBenefitAvailableDateString)
      doc.select("#car-benefit-date-registered").text should be (BenefitFixture.carBenefitRegisteredDateString)
      doc.select("#car-benefit-car-value").text should be (BenefitFixture.carValuePounds)
      doc.select("#car-benefit-employee-capital-contribution").text should be (BenefitFixture.carEmployeeCapitalContributionVauePounds)
      doc.select("#car-benefit-employee-payments").text should be (BenefitFixture.carEmployeePrivateUseContributionVauePounds)
      doc.select("#car-co2-emissions") should be (empty)
      doc.select("#car-benefit-engine") should be (empty)
      doc.select("#car-benefit-fuel-type").text should be (Messages(s"paye.add_car_benefit.fuel_type.${anElectricCar.fuelType}"))
      doc.select("#no-car-benefit-container") should be (empty)
      doc.select("#private-fuel") should be (empty)
    }

  }


}
