package controllers.paye

import play.api.test.{FakeRequest, WithApplication, FakeApplication}
import views.html.paye.{add_car_benefit_review, add_car_benefit_confirmation_table}
import org.jsoup.Jsoup
import play.api.test.Helpers._
import scala.collection.JavaConversions
import JavaConversions._
import play.api.i18n.Messages
import play.api.mvc.Content
import uk.gov.hmrc.common.microservice.paye.domain.AddCarBenefitConfirmationData
import scala.Some


class AddCarBenefitConfirmationTableTemplateSpec extends PayeBaseSpec with MockedTaxYearSupport {

  def documentOf(content: Content) = Jsoup.parse(contentAsString(content))

  "the review add car benefit table" should {
    def addCarBenefitConfirmationDataForEmployeeContribution(employeeContribution: Option[Int]): AddCarBenefitConfirmationData = {
      AddCarBenefitConfirmationData(
        "", LocalDate.apply(), 0, "", None, None, None, None,
        employeeContribution, None)
    }

    "display the contribution to the price of the car when entered" in new WithApplication(FakeApplication()) {
      // given
      val confirmationData = addCarBenefitConfirmationDataForEmployeeContribution(Some(1234))

      // when
      val doc = documentOf(add_car_benefit_review(
        confirmationData, johnDensmore, "", 0, 0)(FakeRequest()))

      // then
      withClue("row with id=employee_contributions") {
        doc.select("#employee_contributions") should not be (empty)
        doc.select("#employee_contributions td").text should be("£1,234")
      }
    }

    "display a \"not selected\" on the contribution to the price of the car when omitted" in new WithApplication(FakeApplication()) {
      // given
      val confirmationData = addCarBenefitConfirmationDataForEmployeeContribution(None)


      // when
      val doc = documentOf(add_car_benefit_review(
        confirmationData, johnDensmore, "", 0, 0)(FakeRequest()))

      // then
      withClue("row with id=employee_contributions") {
        doc.select("#employee_contributions") should not be (empty)
        doc.select("#employee_contributions td").text should be(Messages("paye.add_car_benefit_review.none"))
      }
    }
  }

  "add car benefit confirmation table template" should {

    implicit val user = johnDensmore

    val data = johnDensmoresNewCarData

    "render a table summarising all the values added" in new WithApplication(FakeApplication()) {
      val doc = Jsoup.parse(contentAsString(add_car_benefit_confirmation_table(data)))

      doc.getElementById("new-company-car-details") should bePresent

      doc.getElementsByTag("th") should have size 8

      val tableData = doc.getElementsByTag("td").toList.map(_.text)
      tableData should have size 8
      tableData should contain("8 January 2014")
      tableData should contain("£1,234")
      tableData should contain("Diesel")
      tableData should contain("222 g/km")
      tableData should contain("1,401cc to 2,000cc")
      tableData should contain("£50")
      tableData should contain("1 January 2000")
      tableData should contain("employerName did pay for fuel for private travel, but stopped paying on 5 February 2014")
    }

    "not render the row containing the co2Figure value if not defined" in new WithApplication(FakeApplication()) {
      val doc = documentOf(add_car_benefit_confirmation_table(data.copy(co2Figure = None)))

      doc.getElementById("new-company-car-details") should bePresent

      doc.getElementsByTag("th").toList.map(_.text) should not contain (Messages("paye.add_car_benefit_review.emissions"))
    }

    "not render the row containing the engineCapacity value if not defined" in new WithApplication(FakeApplication()) {
      val doc = documentOf(add_car_benefit_confirmation_table(data.copy(engineCapacity = None)))

      doc.getElementById("new-company-car-details") should bePresent

      doc.getElementsByTag("th").toList.map(_.text) should not contain (Messages("paye.add_car_benefit_review.engine_capacity"))
    }

    "not render the row containing the dateRegistered value if not defined" in new WithApplication(FakeApplication()) {
      val doc = documentOf(add_car_benefit_confirmation_table(data.copy(dateRegistered = None)))

      doc.getElementById("new-company-car-details") should bePresent

      doc.getElementsByTag("th").toList.map(_.text) should not contain (Messages("paye.add_car_benefit_review.date_registered"))
    }

    "not render the row containing the employerPayFuel value if not defined" in new WithApplication(FakeApplication()) {
      val doc = documentOf(add_car_benefit_confirmation_table(data.copy(employerPayFuel = None)))

      doc.getElementById("new-company-car-details") should bePresent

      doc.getElementsByTag("th").toList.map(_.text) should not contain (Messages("paye.add_car_benefit_review.employer_pay_fuel"))
    }
  }

}
