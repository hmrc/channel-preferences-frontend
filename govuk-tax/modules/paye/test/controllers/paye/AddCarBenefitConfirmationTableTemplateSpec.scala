package controllers.paye

import play.api.test.WithApplication
import views.html.paye.add_car_benefit_confirmation_table
import org.jsoup.Jsoup
import play.api.test.Helpers._
import play.api.test.FakeApplication
import scala.collection.JavaConversions
import JavaConversions._
import play.api.i18n.Messages


class AddCarBenefitConfirmationTableTemplateSpec extends PayeBaseSpec with MockedTaxYearSupport {

  "add car benefit confirmation table template" should {

    implicit val user = johnDensmore

    val data =johnDensmoresNewCarData

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
      val doc = Jsoup.parse(contentAsString(add_car_benefit_confirmation_table(data.copy(co2Figure = None))))

      doc.getElementById("new-company-car-details") should bePresent

      doc.getElementsByTag("th") should have size 7
      doc.getElementsByTag("th").toList.map(_.text) should not contain (Messages("paye.add_car_benefit_review.emissions"))
      doc.getElementsByTag("td") should have size 7
    }

    "not render the row containing the engineCapacity value if not defined" in new WithApplication(FakeApplication()) {
      val doc = Jsoup.parse(contentAsString(add_car_benefit_confirmation_table(data.copy(engineCapacity = None))))

      doc.getElementById("new-company-car-details") should bePresent

      doc.getElementsByTag("th") should have size 7
      doc.getElementsByTag("th").toList.map(_.text) should not contain (Messages("paye.add_car_benefit_review.engine_capacity"))
      doc.getElementsByTag("td") should have size 7
    }

    "not render the row containing the employeeContributions value if not defined" in new WithApplication(FakeApplication()) {
      val doc = Jsoup.parse(contentAsString(add_car_benefit_confirmation_table(data.copy(employeeContributions = None))))

      doc.getElementById("new-company-car-details") should bePresent

      doc.getElementsByTag("th") should have size 7
      doc.getElementsByTag("th").toList.map(_.text) should not contain (Messages("paye.add_car_benefit_review.employee_contributions"))
      doc.getElementsByTag("td") should have size 7
    }

    "not render the row containing the dateRegistered value if not defined" in new WithApplication(FakeApplication()) {
      val doc = Jsoup.parse(contentAsString(add_car_benefit_confirmation_table(data.copy(dateRegistered = None))))

      doc.getElementById("new-company-car-details") should bePresent

      doc.getElementsByTag("th") should have size 7
      doc.getElementsByTag("th").toList.map(_.text) should not contain (Messages("paye.add_car_benefit_review.date_registered"))
      doc.getElementsByTag("td") should have size 7
    }

    "not render the row containing the employerPayFuel value if not defined" in new WithApplication(FakeApplication()) {
      val doc = Jsoup.parse(contentAsString(add_car_benefit_confirmation_table(data.copy(employerPayFuel = None))))

      doc.getElementById("new-company-car-details") should bePresent

      doc.getElementsByTag("th") should have size 7
      doc.getElementsByTag("th").toList.map(_.text) should not contain (Messages("paye.add_car_benefit_review.employer_pay_fuel"))
      doc.getElementsByTag("td") should have size 7
    }
  }

}
