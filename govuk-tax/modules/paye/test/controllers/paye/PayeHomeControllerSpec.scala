package controllers.paye

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import controllers.common.CookieEncryption
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.microservice.MockMicroServicesForTests
import play.api.test.{ FakeRequest, WithApplication }
import views.formatting.Dates
import play.api.test.Helpers._
import org.mockito.Mockito._
import uk.gov.hmrc.microservice.paye.domain.Employment
import uk.gov.hmrc.microservice.txqueue.TxQueueTransaction
import play.api.test.FakeApplication
import uk.gov.hmrc.microservice.paye.domain.Benefit
import scala.Some
import uk.gov.hmrc.microservice.paye.domain.TaxCode
import org.mockito.Matchers
import org.jsoup.Jsoup

class PayeHomeControllerSpec extends PayeBaseSpec with MockitoSugar with CookieEncryption {

  private lazy val controller = new PayeHomeController with MockMicroServicesForTests

  private def setupMocksForJohnDensmore(taxCodes: Seq[TaxCode], employments: Seq[Employment], benefits: Seq[Benefit],
    acceptedTransactions: List[TxQueueTransaction], completedTransactions: List[TxQueueTransaction]) {
    when(controller.payeMicroService.linkedResource[Seq[TaxCode]]("/paye/AB123456C/tax-codes/2013")).thenReturn(Some(taxCodes))
    when(controller.payeMicroService.linkedResource[Seq[Employment]]("/paye/AB123456C/employments/2013")).thenReturn(Some(employments))
    when(controller.payeMicroService.linkedResource[Seq[Benefit]]("/paye/AB123456C/benefits/2013")).thenReturn(Some(benefits))
    when(controller.txQueueMicroService.transaction(Matchers.matches("^/txqueue/current-status/paye/AB123456C/ACCEPTED/.*"))).thenReturn(Some(acceptedTransactions))
    when(controller.txQueueMicroService.transaction(Matchers.matches("^/txqueue/current-status/paye/AB123456C/COMPLETED/.*"))).thenReturn(Some(completedTransactions))
  }

  "The home method" should {

    "display the name for John Densmore" in new WithApplication(FakeApplication()) {
      controller.resetAll
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val content = requestHomeAction
      content should include("John Densmore")
    }

    "display the tax codes for John Densmore" in new WithApplication(FakeApplication()) {
      controller.resetAll
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val content = requestHomeAction
      content should include("430L")
    }

    "display the employments for John Densmore" in new WithApplication(FakeApplication()) {
      controller.resetAll
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val content = requestHomeAction
      content should include("Weyland-Yutani Corp")
      content should include("899")
      content should include("1212121")
      content should include("July 2, 2013 to October 8, 2013")
      content should include("October 14, 2013 to present")
    }

    "display employer ref when the employer name is missing" in new WithApplication(FakeApplication()) {
      controller.resetAll
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val content = requestHomeAction
      content should include("1212121")
    }

    "display recent transactions for John Densmore" in new WithApplication(FakeApplication()) {
      controller.resetAll
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, testTransactions, testTransactions)

      val doc = Jsoup.parse(requestHomeAction)
      val recentChanges = doc.select(".overview__actions__done").text
      recentChanges should include(s"On ${Dates.formatDate(currentTestDate.toLocalDate)}, you removed your company car benefit from Weyland-Yutani Corp. This is being processed and you will receive a new Tax Code within 2 days.")
      recentChanges should include(s"On ${Dates.formatDate(currentTestDate.toLocalDate)}, you removed your company car benefit from Weyland-Yutani Corp. This has been processed and your new Tax Code is 430L. Weyland-Yutani Corp have been notified.")

      recentChanges should include(s"On ${Dates.formatDate(currentTestDate.toLocalDate)}, you removed your company fuel benefit from Weyland-Yutani Corp. This is being processed and you will receive a new Tax Code within 2 days.")
      recentChanges should include(s"On ${Dates.formatDate(currentTestDate.toLocalDate)}, you removed your company fuel benefit from Weyland-Yutani Corp. This has been processed and your new Tax Code is 430L. Weyland-Yutani Corp have been notified.")
    }

    "display recent transactions for multiple benefit removal for John Densmore" in new WithApplication(FakeApplication()) {
      controller.resetAll
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, multiBenefitTransactions, testTransactions)

      val doc = Jsoup.parse(requestHomeAction)
      doc.select(".overview__actions__done").text should include(s"${Dates.formatDate(currentTestDate.toLocalDate)}, you removed your company car and fuel benefit from Weyland-Yutani Corp.")
    }

    "return the link to the list of benefits for John Densmore" in new WithApplication(FakeApplication()) {
      controller.resetAll
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val content = requestHomeAction
      content should include("Click here to see your benefits")
    }

    def requestHomeAction: String = {
      val homeAction = controller.homeAction
      val result = homeAction(johnDensmore, FakeRequest())
      status(result) should be(200)
      contentAsString(result)
    }
  }
}
