package controllers.paye

import uk.gov.hmrc.common.BaseSpec
import views.PageSugar
import org.joda.time.{ LocalDate, DateTime }
import views.html.paye.{ paye_benefit_home, paye_home }
import play.api.templates.Html
import uk.gov.hmrc.microservice.paye.domain.{ Benefit, Employment }

class PayeTemplatesSpec extends BaseSpec with PageSugar {

  "Tax overview page" should {

    "display a user name" in {

      val overview: PayeOverview = PayeOverview("John Densmore", Some(new DateTime()), "CS700100A",
        List(EmploymentView("Sainsbury's", new LocalDate(2010, 2, 22), None, "277T", List(RecentChange("something", new LocalDate(2013, 2, 22)))), EmploymentView("Bla Bla", new LocalDate(2009, 4, 11), Some(new LocalDate(2010, 2, 21)), "BR", List())), true)
      val payeHome: Html = paye_home(overview)

      payeHome("h2.welcome").text() must include("John Densmore")

    }

    "recent changes must contain a list element" in {

      val overview: PayeOverview = PayeOverview("John Densmore", Some(new DateTime()), "CS700100A",
        List(EmploymentView("Sainsbury's", new LocalDate(2010, 2, 22), None, "277T", List(RecentChange("something", new LocalDate(2013, 2, 22)))), EmploymentView("Bla Bla", new LocalDate(2009, 4, 11), Some(new LocalDate(2010, 2, 21)), "BR", List())), true)
      val payeHome: Html = paye_home(overview)

      payeHome(".overview__actions__done").html() must include("<li>")

    }

    "display a nino" in {

      val overview: PayeOverview = PayeOverview("John Densmore", Some(new DateTime()), "CS700100A",
        List(EmploymentView("Sainsbury's", new LocalDate(2010, 2, 22), None, "277T", List(RecentChange("something", new LocalDate(2013, 2, 22)))), EmploymentView("Bla Bla", new LocalDate(2009, 4, 11), Some(new LocalDate(2010, 2, 21)), "BR", List())), true)
      val payeHome: Html = paye_home(overview)

      payeHome(".overview__contacts__nino").text() must include("CS700100A")

    }

    "render employments together with taxcodes" in {

      val overview: PayeOverview = PayeOverview("John Densmore", Some(new DateTime()), "CS700100A",
        List(EmploymentView("Sainsbury's", new LocalDate(2010, 2, 22), None, "277T", List(RecentChange("something", new LocalDate(2013, 2, 22)))), EmploymentView("Bla Bla", new LocalDate(2009, 4, 11), Some(new LocalDate(2010, 2, 21)), "BR", List())), true)
      val payeHome: Html = paye_home(overview)

      payeHome(".overview__contacts__heading").first().text() must include("Sainsbury's")
      payeHome(".overview__contacts__companies li").first().child(4).text() must include("277T")

    }

    "display a link to benefits page" in {

      val overview: PayeOverview = PayeOverview("John Densmore", Some(new DateTime()), "CS700100A",
        List(EmploymentView("Sainsbury's", new LocalDate(2010, 2, 22), None, "277T", List(RecentChange("something", new LocalDate(2013, 2, 22)))), EmploymentView("Bla Bla", new LocalDate(2009, 4, 11), Some(new LocalDate(2010, 2, 21)), "BR", List())), true)
      val payeHome: Html = paye_home(overview)

      println(payeHome(".overview__contacts p").last().html())

      payeHome(".overview__contacts p").last().html() must include("href")

    }

  }

  "Paye benefits home page" should {

    "include the hyphenated benefit type as the table row id" in {
      val displayBenefit = DisplayBenefit(
        Employment(1, LocalDate.now(), None, "123", "934503945834", None),
        Benefit(29, 2013, BigDecimal("100.00"), 1, null, null, null, null, null, "Description", None, Map.empty, Map.empty),
        None,
        None
      )

      val page = paye_benefit_home(Seq(displayBenefit))
      println(page)
      page("tr#car-fuel-benefit").size must be(1)
    }
  }
}
