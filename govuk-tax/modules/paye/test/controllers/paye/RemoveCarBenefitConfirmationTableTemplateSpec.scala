package controllers.paye

import play.api.test.WithApplication
import views.html.paye.{remove_car_benefit_confirmation_table, add_car_benefit_confirmation_table}
import org.jsoup.Jsoup
import play.api.test.Helpers._
import play.api.test.FakeApplication
import scala.Some
import uk.gov.hmrc.common.microservice.paye.domain.AddCarBenefitConfirmationData
import org.scalatest.LoneElement
import scala.collection.JavaConversions
import JavaConversions._
import play.api.i18n.Messages
import org.joda.time.LocalDate
import org.joda.time.chrono.ISOChronology
import models.paye.{RemoveFuelBenefitFormData, RemoveCarBenefitFormData}


class RemoveCarBenefitConfirmationTableTemplateSpec extends PayeBaseSpec with StubTaxYearSupport with LoneElement {

  "add car benefit confirmation table template" should {

    implicit val user = johnDensmore

    val formData = johnDensmoresRemovedCarBenefitFormData
    val employment = johnDensmoresEmployments(0)
    val hasFuelBenefit = true


    "render a table summarising all the values added" in new WithApplication(FakeApplication()) {
      val doc = Jsoup.parse(contentAsString(remove_car_benefit_confirmation_table(formData, employment, hasFuelBenefit, testTaxYear)))

      doc.getElementById("company-car-details") should bePresent

      val companyName = doc.getElementById("company-name")
      companyName should bePresent
      companyName.text shouldBe s"Company car provided by ${employment.employerNameOrReference}"

      doc.getElementsByTag("th") should have size 4
      doc.getElementsByTag("td") should have size 4

      doc.getElementById("car-benefit-date-car-withdrawn").text shouldBe "8 January 2014"
      doc.getElementById("car-benefit-num-days-unavailable").text shouldBe "10 days"
      doc.getElementById("car-benefit-employee-payments").text shouldBe "£2,000"
      doc.getElementById("car-benefit-date-fuel-withdrawn").text shouldBe "8 February 2013"
    }

    "not render the date fuel withdrawn date if there no fuel benefit" in new WithApplication(FakeApplication()) {
      val doc = Jsoup.parse(contentAsString(remove_car_benefit_confirmation_table(formData, employment, false, testTaxYear)))

      doc.getElementById("company-car-details") should bePresent

      val companyName = doc.getElementById("company-name")
      companyName should bePresent
      companyName.text shouldBe s"Company car provided by ${employment.employerNameOrReference}"

      doc.getElementsByTag("th") should have size 3
      doc.getElementsByTag("td") should have size 3

      doc.getElementById("car-benefit-date-car-withdrawn").text shouldBe "8 January 2014"
      doc.getElementById("car-benefit-num-days-unavailable").text shouldBe "10 days"
      doc.getElementById("car-benefit-employee-payments").text shouldBe "£2,000"
      doc.getElementById("car-benefit-date-fuel-withdrawn") shouldBe null
    }
  }

}
