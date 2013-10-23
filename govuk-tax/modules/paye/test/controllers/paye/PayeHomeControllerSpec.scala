package controllers.paye

import org.scalatest.mock.MockitoSugar
import controllers.common.CookieEncryption
import play.api.test.{FakeRequest, WithApplication}
import play.api.test.Helpers._
import org.mockito.Mockito._
import uk.gov.hmrc.common.microservice.paye.domain.Employment
import uk.gov.hmrc.common.microservice.paye.domain.Benefit
import org.mockito.Matchers
import org.jsoup.Jsoup
import uk.gov.hmrc.microservice.txqueue.TxQueueTransaction
import scala.Some
import play.api.test.FakeApplication
import uk.gov.hmrc.common.microservice.paye.domain.TaxCode
import uk.gov.hmrc.common.microservice.MockMicroServicesForTests
import concurrent.Future

class PayeHomeControllerSpec
  extends PayeBaseSpec
  with MockitoSugar
  with CookieEncryption {

  private implicit val controller = new PayeHomeController with MockMicroServicesForTests

  before {
    setupMocksForJohnDensmore(
      taxCodes = johnDensmoresTaxCodes,
      employments = johnDensmoresEmployments,
      benefits = johnDensmoresBenefits,
      acceptedTransactions = List.empty,
      completedTransactions = List.empty)
  }

  "The home method" should {

    "display the name for John Densmore" in new WithApplication(FakeApplication()) {
      val content = requestHomeAction
      content should include("John Densmore")
    }

    "display the tax codes for John Densmore" in new WithApplication() {
      val content = requestHomeAction
      content should include("430L")
    }

    "display the employments for John Densmore" in new WithApplication(FakeApplication()) {
      val content = requestHomeAction
      content should include("Weyland-Yutani Corp")
      content should include("899")
      content should include("1212121")
      content should include("2 July 2013 to 8 October 2013")
      content should include("14 October 2013 to present")
    }

    "display employer ref when the employer name is missing" in new WithApplication(FakeApplication()) {
      val content = requestHomeAction
      content should include("1212121")
    }

    "display recent transactions for John Densmore" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, testTransactions, testTransactions)

      val doc = Jsoup.parse(requestHomeAction)
      val recentChanges = doc.select(".overview__actions__done").text
      recentChanges should include(s"On 2 December 2012, you removed your company car benefit from Weyland-Yutani Corp. This is being processed and you will receive a new Tax Code within 2 days.")
      recentChanges should include(s"On 2 December 2012, you removed your company car benefit from Weyland-Yutani Corp. This has been processed and your new Tax Code is 430L. Weyland-Yutani Corp have been notified.")

      recentChanges should include(s"On 2 December 2012, you removed your company fuel benefit from Weyland-Yutani Corp. This is being processed and you will receive a new Tax Code within 2 days.")
      recentChanges should include(s"On 2 December 2012, you removed your company fuel benefit from Weyland-Yutani Corp. This has been processed and your new Tax Code is 430L. Weyland-Yutani Corp have been notified.")

      doc.select(".no_actions") shouldBe empty
    }

    "display recent transactions for multiple benefit removal for John Densmore" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, multiBenefitTransactions, multiBenefitTransactions)

      val doc = Jsoup.parse(requestHomeAction)
      doc.select(".overview__actions__done").text should include(s"2 December 2012, you removed your company car and fuel benefit from Weyland-Yutani Corp.")
      doc.select(".overview__actions__done").text should include(s"2 December 2012, you removed your company car and fuel benefit from Weyland-Yutani Corp. This has been processed and your new Tax Code is 430L. Weyland-Yutani Corp have been notified.")
    }

    "display a message for John Densmore if there are no transactions" in new WithApplication(FakeApplication()) {
      val doc = Jsoup.parse(requestHomeAction)
      doc.select(".no_actions") should not be empty
      doc.select(".no_actions").text should include("no changes")
    }

    "return the link to the list of benefits for John Densmore" in new WithApplication(FakeApplication()) {
      val content = requestHomeAction
      content should include("Click here to see your benefits")
    }

    def requestHomeAction: String = {
      val result = Future.successful(controller.homeAction(FakeRequest())(johnDensmore))
      status(result) should be(200)
      contentAsString(result)
    }
  }


  private def setupMocksForJohnDensmore(taxCodes: Seq[TaxCode],
                                        employments: Seq[Employment],
                                        benefits: Seq[Benefit],
                                        acceptedTransactions: List[TxQueueTransaction],
                                        completedTransactions: List[TxQueueTransaction]) {
    controller.resetAll()
    when(controller.payeMicroService.linkedResource[Seq[TaxCode]]("/paye/AB123456C/tax-codes/2013")).thenReturn(Some(taxCodes))
    when(controller.payeMicroService.linkedResource[Seq[Employment]]("/paye/AB123456C/employments/2013")).thenReturn(Some(employments))
    when(controller.payeMicroService.linkedResource[Seq[Benefit]]("/paye/AB123456C/benefits/2013")).thenReturn(Some(benefits))
    when(controller.txQueueMicroService.transaction(Matchers.matches("^/txqueue/current-status/paye/AB123456C/ACCEPTED/.*"))).thenReturn(Some(acceptedTransactions))
    when(controller.txQueueMicroService.transaction(Matchers.matches("^/txqueue/current-status/paye/AB123456C/COMPLETED/.*"))).thenReturn(Some(completedTransactions))
  }
}


