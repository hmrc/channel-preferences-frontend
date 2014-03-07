package controllers.paye

import scala.collection.JavaConversions._
import org.jsoup.Jsoup

import play.api.test.{FakeRequest, WithApplication, FakeApplication}
import play.api.i18n.Messages
import play.api.test.Helpers._
import play.api.mvc.Content

import uk.gov.hmrc.common.microservice.paye.domain.AddCarBenefitConfirmationData

import views.html.paye.{add_car_benefit_review, add_car_benefit_confirmation_table}

class AddCarBenefitConfirmationTableTemplateSpec extends PayeBaseSpec with TaxYearSupport {

  def documentOf(content: Content) = Jsoup.parse(contentAsString(content))

  "the review add car benefit table" should {
    def addCarBenefitConfirmationDataForEmployeeContribution(employeeContribution: Option[Int], employerContribution: Option[Int] = None): AddCarBenefitConfirmationData = {
      AddCarBenefitConfirmationData("", LocalDate.apply(), 0, "", None, None, None, employeeContribution, None, employerContribution)
    }

    "display the contribution to the price of the car when entered" in new WithApplication(FakeApplication()) {
      // given
      val confirmationData = addCarBenefitConfirmationDataForEmployeeContribution(Some(1234))

      // when
      val doc = documentOf(add_car_benefit_review(
        confirmationData, johnDensmore, 0, 0)(FakeRequest()))

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
        confirmationData, johnDensmore, 0, 0)(FakeRequest()))

      // then
      withClue("row with id=employee_contributions") {
        doc.select("#employee_contributions") should not be empty
        doc.select("#employee_contributions td").text should be(Messages("paye.add_car_benefit_review.none"))
      }
    }

    "Display None in fields that are not selected" in {
      val data = addCarBenefitConfirmationDataForEmployeeContribution(None)

      val doc = Jsoup.parse(contentAsString(add_car_benefit_confirmation_table(data)))
      doc.getElementById("new-company-car-details") should bePresent

      val tableData = doc.getElementsByTag("td").toList.map(_.text)

      withClue("add car benefit review table") {
        doc.getElementsByTag("th") should have size 6
        tableData.filter(_.equals(Messages("paye.add_car_benefit_review.none"))) should have size 3
      }
    }
  }

  "add car benefit confirmation table template" should {

    implicit val user = johnDensmore

    val data = johnDensmoresNewCarData

    "render a table summarising all the values added" in new WithApplication(FakeApplication()) {
      val doc = Jsoup.parse(contentAsString(add_car_benefit_confirmation_table(data)))

      doc.getElementById("new-company-car-details") should bePresent

      doc.getElementsByTag("th") should have size 9

      val tableData = doc.getElementsByTag("td").toList.map(_.text)
      tableData should have size 9
      tableData should contain(s"8 January ${testTaxYear+1}")
      tableData should contain("£1,234")
      tableData should contain("Diesel")
      tableData should contain("222 g/km")
      tableData should contain("1,401cc to 2,000cc")
      tableData should contain("£50")
      tableData should contain("£300")
      tableData should contain("1 January 2000")
    }
  }

}
