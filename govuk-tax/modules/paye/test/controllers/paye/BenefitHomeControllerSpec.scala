package controllers.paye

import org.scalatest.mock.MockitoSugar
import controllers.common.CookieEncryption
import uk.gov.hmrc.common.microservice.MockMicroServicesForTests
import play.api.test.{ FakeRequest, WithApplication }
import org.mockito.Mockito._
import uk.gov.hmrc.common.microservice.paye.domain._
import org.mockito.Matchers
import uk.gov.hmrc.common.microservice.domain.User
import play.api.test.Helpers._
import uk.gov.hmrc.common.microservice.paye.domain.Employment
import play.api.test.FakeApplication
import uk.gov.hmrc.common.microservice.paye.domain.Benefit
import scala.Some
import uk.gov.hmrc.microservice.txqueue.TxQueueTransaction
import uk.gov.hmrc.common.microservice.paye.PayeMicroService
import org.scalatest.TestData
import concurrent.Future

class BenefitHomeControllerSpec extends PayeBaseSpec with MockitoSugar with CookieEncryption {

  private val mockPayeService = mock[PayeMicroService]

  private lazy val controller = new BenefitHomeController(mockPayeService) with MockMicroServicesForTests

  private def setupMocksForJohnDensmore(taxCodes: Seq[TaxCode], employments: Seq[Employment], benefits: Seq[Benefit],
    acceptedTransactions: List[TxQueueTransaction], completedTransactions: List[TxQueueTransaction]) {
    when(controller.payeMicroService.linkedResource[Seq[TaxCode]]("/paye/AB123456C/tax-codes/2013")).thenReturn(Some(taxCodes))
    when(controller.payeMicroService.linkedResource[Seq[Employment]]("/paye/AB123456C/employments/2013")).thenReturn(Some(employments))
    when(controller.payeMicroService.linkedResource[Seq[Benefit]]("/paye/AB123456C/benefits/2013")).thenReturn(Some(benefits))
    when(controller.txQueueMicroService.transaction(Matchers.matches("^/txqueue/current-status/paye/AB123456C/ACCEPTED/.*"))).thenReturn(Some(acceptedTransactions))
    when(controller.txQueueMicroService.transaction(Matchers.matches("^/txqueue/current-status/paye/AB123456C/COMPLETED/.*"))).thenReturn(Some(completedTransactions))
  }

  override protected def beforeEach(testData: TestData) {
    super.beforeEach(testData)

    controller.resetAll()
    reset(mockPayeService)
  }


  "The benefits list page" should {

    "display John s benefits" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      requestBenefitsAction(johnDensmore) should include("&pound;135")
    }

    "not display a benefits without a corresponding employment" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresOneEmployment(), johnDensmoresBenefits, List.empty, List.empty)

      requestBenefitsAction(johnDensmore) should not include "22"
    }

    "display car details" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      requestBenefitsAction(johnDensmore) should include("Medical Insurance")
      requestBenefitsAction(johnDensmore) should include("Car Benefit")
      requestBenefitsAction(johnDensmore) should include("899/1212121")
      requestBenefitsAction(johnDensmore) should include("Engine size: 0-1400 cc")
      requestBenefitsAction(johnDensmore) should include("Fuel type: Bi-Fuel")
      requestBenefitsAction(johnDensmore) should include("Date car registered: 12 December 2012")
      requestBenefitsAction(johnDensmore) should include("&pound;321")
    }

    "display a remove link for car benefits" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      requestBenefitsAction(johnDensmore) should include("""href="/benefits/31/2013/2/remove"""")
    }

    "display a Car removed if there is an accepted transaction present for the car benefit" in new WithApplication(FakeApplication()) {
      when(controller.payeMicroService.linkedResource[Seq[Employment]]("/paye/RC123456B/employments/2013")).thenReturn(userWithRemovedCarEmployments)
      when(controller.payeMicroService.linkedResource[Seq[Benefit]]("/paye/RC123456B/benefits/2013")).thenReturn(userWithRemovedCarBenefits)
      when(controller.txQueueMicroService.transaction(Matchers.matches("^/txqueue/current-status/paye/RC123456B/ACCEPTED/.*"))).thenReturn(Some(acceptedTransactions))
      when(controller.txQueueMicroService.transaction(Matchers.matches("^/txqueue/current-status/paye/RC123456B/COMPLETED/.*"))).thenReturn(Some(List.empty))

      val result = requestBenefitsAction(userWithRemovedCar)
      result should include("Benefit removed")
    }

    "display a benefit removed if there is a completed transaction present for the car benefit" in new WithApplication(FakeApplication()) {
      when(controller.payeMicroService.linkedResource[Seq[Employment]]("/paye/RC123456B/employments/2013")).thenReturn(userWithRemovedCarEmployments)
      when(controller.payeMicroService.linkedResource[Seq[Benefit]]("/paye/RC123456B/benefits/2013")).thenReturn(userWithRemovedCarBenefits)
      when(controller.txQueueMicroService.transaction(Matchers.matches("^/txqueue/current-status/paye/RC123456B/ACCEPTED/.*"))).thenReturn(Some(List.empty))
      when(controller.txQueueMicroService.transaction(Matchers.matches("^/txqueue/current-status/paye/RC123456B/COMPLETED/.*"))).thenReturn(Some(List(removedCarTransaction)))

      val result = requestBenefitsAction(userWithRemovedCar)
      result should include("Benefit removed")
    }

    def requestBenefitsAction(user: User) = {
      val result = Future.successful(controller.listBenefitsAction(user, FakeRequest()))
      status(result) shouldBe 200
      contentAsString(result)
    }

  }
}
