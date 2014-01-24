package controllers.paye

import views.html.paye.display_car_and_fuel
import uk.gov.hmrc.common.microservice.paye.domain.{FuelBenefit, CarBenefit}
import uk.gov.hmrc.common.BaseSpec
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import play.api.test.Helpers._
import play.api.i18n.Messages

class DisplayCarAndFuelTemplateSpec extends BaseSpec {
  private val employerName = "Tesco"
  private val fuelBenefit = FuelBenefit(
    dateWithdrawn = Some(new LocalDate(2013, 5, 5)),
    benefitAmount = BigDecimal(1500),
    grossAmount = BigDecimal(2000),
    startDate = new LocalDate(2012, 5, 5)
  )

  private val carBenefit = new CarBenefit(
    taxYear = 2013,
    employmentSequenceNumber = 2,
    startDate = new LocalDate(2012, 5, 5),
    benefitAmount = BigDecimal(6000),
    grossAmount = BigDecimal(8000),
    dateMadeAvailable = new LocalDate(2012, 5, 5),
    dateWithdrawn = Some(new LocalDate(2013, 5, 5)),
    dateCarRegistered = new LocalDate(2003, 5, 5),
    carValue = BigDecimal(25000),
    employeeCapitalContribution = BigDecimal(1000),
    employeePayments = BigDecimal(200),
    fuelType = "1",
    fuelBenefit = Some(fuelBenefit),
    co2Emissions = Some(120),
    engineSize = Some(2200),
    daysUnavailable = Some(32)
  )


  "rendering car and fuel benefit" should {
    "display all the details in a car with every property" in {
      val result = display_car_and_fuel(carBenefit, employerName)
      val doc = Jsoup.parse(contentAsString(result))

      doc.getElementById("company-name").text shouldBe Messages("paye.display_car_and_fuel.company_car_provided_by", "Tesco")
      doc.getElementById("car-benefit-date-available").text shouldBe "5 May 2012"
      doc.getElementById("car-benefit-date-withdrawn").text shouldBe "5 May 2013"
      doc.getElementById("car-benefit-date-registered").text shouldBe "5 May 2003"
      doc.getElementById("car-benefit-days-unavailable").text shouldBe "32"
      doc.getElementById("car-benefit-car-value").text shouldBe "£25,000"
      doc.getElementById("car-benefit-employee-capital-contribution").text shouldBe "£1,000"
      doc.getElementById("car-benefit-employee-payments").text shouldBe "£200"
      doc.getElementById("private-fuel").text shouldBe Messages("paye.home.employer_pay_fuel.date")
      doc.getElementById("car-benefit-fuel-type").text shouldBe Messages("paye.add_car_benefit.fuel_type.1")
      doc.getElementById("car-co2-emissions").text shouldBe Messages("paye.display_car_and_fuel.emissions_unit", 120)
      doc.getElementById("car-benefit-engine").text shouldBe Messages("paye.add_car_benefit.engine_capacity.2200")
    }

    "not display the optional fields ig they are not defined" in {
      val result = display_car_and_fuel(carBenefit.copy(dateWithdrawn = None, fuelBenefit = None, co2Emissions = None, engineSize = None, daysUnavailable = None), employerName)
      val doc = Jsoup.parse(contentAsString(result))

      doc.getElementById("company-name").text shouldBe Messages("paye.display_car_and_fuel.company_car_provided_by", "Tesco")
      doc.getElementById("car-benefit-date-available").text shouldBe "5 May 2012"
      doc.getElementById("car-benefit-date-withdrawn") shouldBe null
      doc.getElementById("car-benefit-date-registered").text shouldBe "5 May 2003"
      doc.getElementById("car-benefit-days-unavailable") shouldBe null
      doc.getElementById("car-benefit-car-value").text shouldBe "£25,000"
      doc.getElementById("car-benefit-employee-capital-contribution").text shouldBe "£1,000"
      doc.getElementById("car-benefit-employee-payments").text shouldBe "£200"
      doc.getElementById("private-fuel").text shouldBe Messages("paye.home.employer_pay_fuel.false")
      doc.getElementById("car-benefit-fuel-type").text shouldBe Messages("paye.add_car_benefit.fuel_type.1")
      doc.getElementById("car-co2-emissions") shouldBe null
      doc.getElementById("car-benefit-engine") shouldBe null
    }
  }

}
