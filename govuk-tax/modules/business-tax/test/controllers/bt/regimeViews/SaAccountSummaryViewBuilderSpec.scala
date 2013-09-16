package controllers.bt.regimeViews

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.microservice.sa.SaMicroService
import org.mockito.Mockito._
import views.helpers._
import org.joda.time.LocalDate
import controllers.bt.routes
import controllers.bt.regimeViews.SaAccountSummaryMessageKeys._
import views.helpers.LinkMessage
import uk.gov.hmrc.microservice.sa.domain.Liability
import uk.gov.hmrc.microservice.sa.domain.SaRoot
import views.helpers.RenderableLinkMessage
import scala.Some
import views.helpers.RenderableStringMessage
import uk.gov.hmrc.microservice.sa.domain.SaAccountSummary
import uk.gov.hmrc.microservice.domain.User
import uk.gov.hmrc.microservice.domain.RegimeRoots
import uk.gov.hmrc.microservice.sa.domain.AmountDue

class SaAccountSummaryViewBuilderSpec extends BaseSpec with MockitoSugar {

  trait DummyPortalUrlBuilder {
    def build(a: String): String
  }

  val viewAccountDetailsUrl = "http://viewAccountDetails"
  val fileAReturnUrl = "http://fileAReturn"
  val homeUrl = "http://homeUrl"
  val makeAPaymentUrl = routes.BusinessTaxController.makeAPaymentLanding().url
  val liabilityDate = new LocalDate(2014, 1, 15)

  "Sa Account SummaryView Builder builds correct Account Summary model " should {
    " when no amounts are due now or later " in {
      val totalAmountDueToHmrc = None
      val nextPayment = None
      val amountHmrcOwe = None

      val accountSummary = SaAccountSummary(totalAmountDueToHmrc, nextPayment, amountHmrcOwe)
      val expectedMessages: Seq[(String, List[RenderableMessage])] =
        Seq(
          (nothingToPay, List.empty),
          (viewHistory, List.empty)
        )

      testSaAccountSummaryBuilder(accountSummary, expectedMessages)
    }

    "when an amount is due now (payable) " in {
      val amountDue = BigDecimal(100)
      val totalAmountDueToHmrc = Some(AmountDue(amountDue, true))
      val nextPayment = None
      val amountHmrcOwe = Some(BigDecimal(0))

      val accountSummary = SaAccountSummary(totalAmountDueToHmrc, nextPayment, amountHmrcOwe)
      val expectedMessages = Seq(
        (amountDueForPayment, List(RenderableMoneyMessage(amountDue))),
        (interestApplicable, List.empty)
      )

      testSaAccountSummaryBuilder(accountSummary, expectedMessages)

    }

    "when amounts are due now and later " in {
      val amountDue = BigDecimal(100)
      val totalAmountDueToHmrc = Some(AmountDue(amountDue, true))

      val liabilityAmount = BigDecimal(20)
      val nextPayment = Some(Liability(new LocalDate(2014, 1, 15), liabilityAmount))
      val amountHmrcOwe = Some(BigDecimal(0))

      val accountSummary = SaAccountSummary(totalAmountDueToHmrc, nextPayment, amountHmrcOwe)
      val expectedMessages = Seq(
        (amountDueForPayment, List(RenderableMoneyMessage(amountDue))),
        (interestApplicable, List.empty),
        (willBecomeDue, List(RenderableMoneyMessage(liabilityAmount), RenderableDateMessage(liabilityDate)))
      )

      testSaAccountSummaryBuilder(accountSummary, expectedMessages)

    }

    "when an amount is due for repayment to the user " in {
      val totalAmountDueToHmrc = None
      val nextPayment = None
      val amountHmrcOwe = Some(BigDecimal(100))

      val accountSummary = SaAccountSummary(totalAmountDueToHmrc, nextPayment, amountHmrcOwe)
      val expectedMessages = Seq(
        (youHaveOverpaid, List.empty),
        (amountDueForRepayment, List(RenderableMoneyMessage(amountHmrcOwe.get))),
        (viewHistory, List.empty)
      )

      testSaAccountSummaryBuilder(accountSummary, expectedMessages)

    }

    "when an amount is due for repayment to the user and an amount is becoming due for repayment " in {
      val totalAmountDueToHmrc = None
      val liabilityAmount = BigDecimal(20)
      val nextPayment = Some(Liability(new LocalDate(2014, 1, 15), liabilityAmount))
      val amountHmrcOwe = Some(BigDecimal(100))

      val accountSummary = SaAccountSummary(totalAmountDueToHmrc, nextPayment, amountHmrcOwe)
      val expectedMessages = Seq(
        (youHaveOverpaid, List.empty),
        (amountDueForRepayment, List(RenderableMoneyMessage(amountHmrcOwe.get))),
        (willBecomeDue, List(RenderableMoneyMessage(liabilityAmount), RenderableDateMessage(liabilityDate)))
      )
      testSaAccountSummaryBuilder(accountSummary, expectedMessages)
    }

    "when no amounts due currently but amounts becoming due for payment " in {
      val totalAmountDueToHmrc = None
      val liabilityAmount = BigDecimal(20)
      val nextPayment = Some(Liability(new LocalDate(2014, 1, 15), liabilityAmount))
      val amountHmrcOwe = None

      val accountSummary = SaAccountSummary(totalAmountDueToHmrc, nextPayment, amountHmrcOwe)
      val expectedMessages = Seq(
        (nothingToPay, List.empty),
        (willBecomeDue, List(RenderableMoneyMessage(liabilityAmount), RenderableDateMessage(liabilityDate)))
      )
      testSaAccountSummaryBuilder(accountSummary, expectedMessages)
    }

    "when amount due for payment (not payable) and other becoming due " in {
      val amountDue = BigDecimal(10)
      val totalAmountDueToHmrc = Some(AmountDue(amountDue, false))
      val liabilityAmount = BigDecimal(20)
      val nextPayment = Some(Liability(new LocalDate(2014, 1, 15), liabilityAmount))
      val amountHmrcOwe = None

      val accountSummary = SaAccountSummary(totalAmountDueToHmrc, nextPayment, amountHmrcOwe)
      val expectedMessages = Seq(
        (amountDueForPayment, List(RenderableMoneyMessage(amountDue))),
        (smallAmountToPay, List.empty),
        (willBecomeDue, List(RenderableMoneyMessage(liabilityAmount), RenderableDateMessage(liabilityDate)))
      )
      testSaAccountSummaryBuilder(accountSummary, expectedMessages)
    }

    "when account summary is not available " in {
      val mockUser = mock[User]
      val mockSaMicroService = mock[SaMicroService]
      val mockRegimeRoots = mock[RegimeRoots]
      val mockSaRoot = mock[SaRoot]

      val mockPortalUrlBuilder = mock[DummyPortalUrlBuilder]

      when(mockUser.regimes).thenReturn(mockRegimeRoots)
      when(mockRegimeRoots.sa).thenReturn(Some(mockSaRoot))
      when(mockSaRoot.accountSummary(mockSaMicroService)).thenThrow(new RuntimeException)

      val expectedMessages =
        Seq(
          (unableToDisplayAccount1, List.empty),
          (unableToDisplayAccount2, List.empty),
          (unableToDisplayAccount3, List.empty),
          (unableToDisplayAccount4, List.empty)
        )

      val actualAccountSummary = SaAccountSummaryViewBuilder(mockPortalUrlBuilder.build _, mockUser, mockSaMicroService).build().get
      actualAccountSummary.regimeName shouldBe saRegimeName
      actualAccountSummary.messages shouldBe expectedMessages

    }

  }

  private def testSaAccountSummaryBuilder(accountSummary: SaAccountSummary, expectedMessages: Seq[(String, List[RenderableMessage])]) {
    val mockUser = mock[User]
    val mockSaMicroService = mock[SaMicroService]
    val mockRegimeRoots = mock[RegimeRoots]
    val mockSaRoot = mock[SaRoot]

    val mockPortalUrlBuilder = mock[DummyPortalUrlBuilder]

    when(mockUser.regimes).thenReturn(mockRegimeRoots)
    when(mockRegimeRoots.sa).thenReturn(Some(mockSaRoot))
    when(mockSaRoot.accountSummary(mockSaMicroService)).thenReturn(Some(accountSummary))

    when(mockPortalUrlBuilder.build("home")).thenReturn(homeUrl)
    when(mockPortalUrlBuilder.build(makeAPaymentLink)).thenReturn(makeAPaymentUrl)

    val actualAccountSummary = SaAccountSummaryViewBuilder(mockPortalUrlBuilder.build _, mockUser, mockSaMicroService).build().get

    actualAccountSummary.regimeName shouldBe saRegimeName

    actualAccountSummary.messages shouldBe expectedMessages

    val expectedLinks = Seq(
      RenderableLinkMessage(LinkMessage(homeUrl, viewAccountDetailsLink)),
      RenderableLinkMessage(LinkMessage(makeAPaymentUrl, makeAPaymentLink)),
      RenderableLinkMessage(LinkMessage(homeUrl, fileAReturnLink))

    )
    actualAccountSummary.addenda shouldBe expectedLinks
  }

}