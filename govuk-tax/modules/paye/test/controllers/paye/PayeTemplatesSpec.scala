package controllers.paye

import uk.gov.hmrc.common.BaseSpec
import views.PageSugar
import org.joda.time.{ LocalDate, DateTime }
import views.html.paye.{ paye_benefit_home, paye_home }
import play.api.templates.Html
import uk.gov.hmrc.common.microservice.paye.domain.{ Benefit, Employment }
import models.paye.{ DisplayBenefit, RecentChange, EmploymentView, PayeOverview }
import play.api.test.{FakeApplication, WithApplication}
import uk.gov.hmrc.common.microservice.paye.domain.Employment._
import models.paye.EmploymentView
import models.paye.PayeOverview
import play.api.test.FakeApplication
import models.paye.RecentChange
import scala.Some

class PayeTemplatesSpec extends BaseSpec with PageSugar {

  "Tax overview page" should {

    "display a user name" in new WithApplication(FakeApplication()) {

      val overview: PayeOverview = PayeOverview("John Densmore", Some(new DateTime()), "CS700100A",
        List(EmploymentView("Sainsbury's", new LocalDate(2010, 2, 22), None, "277T", List(RecentChange("something", new LocalDate(2013, 2, 22)))), EmploymentView("Bla Bla", new LocalDate(2009, 4, 11), Some(new LocalDate(2010, 2, 21)), "BR", List())), true)
      val payeHome: Html = paye_home(overview)

      payeHome(".welcome").text() should include("John Densmore")

    }

    "recent changes must contain a list element" in new WithApplication(FakeApplication()) {

      val overview: PayeOverview = PayeOverview("John Densmore", Some(new DateTime()), "CS700100A",
        List(EmploymentView("Sainsbury's", new LocalDate(2010, 2, 22), None, "277T", List(RecentChange("something", new LocalDate(2013, 2, 22)))), EmploymentView("Bla Bla", new LocalDate(2009, 4, 11), Some(new LocalDate(2010, 2, 21)), "BR", List())), true)
      val payeHome: Html = paye_home(overview)

      payeHome(".overview__actions__done").html() should include("<li>")

    }

    "display a nino" in new WithApplication(FakeApplication()) {

      val overview: PayeOverview = PayeOverview("John Densmore", Some(new DateTime()), "CS700100A",
        List(EmploymentView("Sainsbury's", new LocalDate(2010, 2, 22), None, "277T", List(RecentChange("something", new LocalDate(2013, 2, 22)))), EmploymentView("Bla Bla", new LocalDate(2009, 4, 11), Some(new LocalDate(2010, 2, 21)), "BR", List())), true)
      val payeHome: Html = paye_home(overview)

      payeHome(".overview__contacts__nino").text() should include("CS700100A")

    }

    "render employments together with taxcodes" in new WithApplication(FakeApplication()) {

      val overview: PayeOverview = PayeOverview("John Densmore", Some(new DateTime()), "CS700100A",
        List(EmploymentView("Sainsbury's", new LocalDate(2010, 2, 22), None, "277T", List(RecentChange("something", new LocalDate(2013, 2, 22)))), EmploymentView("Bla Bla", new LocalDate(2009, 4, 11), Some(new LocalDate(2010, 2, 21)), "BR", List())), true)
      val payeHome: Html = paye_home(overview)

      payeHome(".overview__contacts__heading").first().text() should include("Sainsbury's")
      payeHome(".overview__contacts__companies li").first().child(4).text() should include("277T")

    }

    "display a link to benefits page" in new WithApplication(FakeApplication()) {

      val overview: PayeOverview = PayeOverview("John Densmore", Some(new DateTime()), "CS700100A",
        List(EmploymentView("Sainsbury's", new LocalDate(2010, 2, 22), None, "277T", List(RecentChange("something", new LocalDate(2013, 2, 22)))), EmploymentView("Bla Bla", new LocalDate(2009, 4, 11), Some(new LocalDate(2010, 2, 21)), "BR", List())), true)
      val payeHome: Html = paye_home(overview)

      println(payeHome(".overview__contacts p").last().html())

      payeHome(".overview__contacts p").last().html() should include("href")

    }

  }

  "Paye benefits home page" should {

    "include the hyphenated benefit type as the table row id" in new WithApplication(FakeApplication()) {
      val displayBenefit = DisplayBenefit(
        Employment(1, LocalDate.now(), None, "123", "934503945834", None, primaryEmploymentType),
        Seq(Benefit(29, 2013, BigDecimal("100.00"), 1, null, null, null, null, null, "Description", None, Map.empty, Map.empty)),
        None,
        None
      )

      val page = paye_benefit_home(Seq(displayBenefit))
      println(page)
      page("tr#car-fuel-benefit").size should be(1)
    }
  }
}
