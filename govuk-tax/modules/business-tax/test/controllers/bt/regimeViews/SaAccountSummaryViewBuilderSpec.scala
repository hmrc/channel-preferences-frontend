package controllers.bt.regimeViews

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.microservice.sa.domain.{Liability, SaRoot, AmountDue, SaAccountSummary}
import uk.gov.hmrc.microservice.sa.SaMicroService
import uk.gov.hmrc.microservice.domain.{RegimeRoots, User}
import org.mockito.Mockito._
import views.helpers.{RenderableLinkMessage, RenderableStringMessage, RenderableMessage, LinkMessage}
import org.joda.time.LocalDate
import controllers.bt.routes
import controllers.bt.regimeViews.SaAccountSummaryMessageKeys

class SaAccountSummaryViewBuilderSpec extends BaseSpec with MockitoSugar {

  trait DummyPortalUrlBuilder {
    def build(a: String): String
  }

  val viewAccountDetailsUrl = "http://viewAccountDetails"
  val fileAReturnUrl = "http://fileAReturn"
  val homeUrl = "http://homeUrl"
  val makeAPaymentUrl = routes.BusinessTaxController.makeAPaymentLanding().url

  "Sa Account SummaryView Builder builds correct Account Summary model " should {
    " when no amounts are due now or later " in {
      val totalAmountDueToHmrc = None
      val nextPayment = None
      val amountHmrcOwe = None

      val accountSummary = SaAccountSummary(totalAmountDueToHmrc, nextPayment, amountHmrcOwe)
      val expectedMessages: Seq[(String, List[RenderableMessage])] =
        Seq(
          ("sa.message.nothingToPay", List.empty),
          ("sa.message.viewHistory", List.empty)
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
        ("sa.message.amountDueForPayment", List(RenderableStringMessage(amountDue.toString()))),
        ("sa.message.interestApplicable", List.empty)
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
        ("sa.message.amountDueForPayment", List(RenderableStringMessage(amountDue.toString()))),
        ("sa.message.interestApplicable", List.empty),
        ("sa.message.willBecomeDue", List(RenderableStringMessage(liabilityAmount.toString()), RenderableStringMessage("2014-01-15")))
      )

      testSaAccountSummaryBuilder(accountSummary, expectedMessages)

    }

    "when an amount is due for repayment to the user " in {
      val totalAmountDueToHmrc = None
      val nextPayment = None
      val amountHmrcOwe = Some(BigDecimal(100))

      val accountSummary = SaAccountSummary(totalAmountDueToHmrc, nextPayment, amountHmrcOwe)
      val expectedMessages = Seq(
        ("sa.message.youHaveOverpaid", List.empty),
        ("sa.message.amountDueForRepayment", List(RenderableStringMessage(amountHmrcOwe.get.toString()))),
        ("sa.message.viewHistory", List.empty)
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
        ("sa.message.youHaveOverpaid", List.empty),
        ("sa.message.amountDueForRepayment", List(RenderableStringMessage(amountHmrcOwe.get.toString()))),
        ("sa.message.willBecomeDue", List(RenderableStringMessage(liabilityAmount.toString()), RenderableStringMessage("2014-01-15")))
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
        ("sa.message.nothingToPay", List.empty),
        ("sa.message.willBecomeDue", List(RenderableStringMessage(liabilityAmount.toString()), RenderableStringMessage("2014-01-15")))
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
        ("sa.message.amountDueForPayment", List(RenderableStringMessage(amountDue.toString()))),
        ("sa.message.smallAmountToPay", List.empty),
        ("sa.message.willBecomeDue", List(RenderableStringMessage(liabilityAmount.toString()), RenderableStringMessage("2014-01-15")))
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
          ("sa.message.unableToDisplayAccount.1", List.empty),
          ("sa.message.unableToDisplayAccount.2", List.empty),
          ("sa.message.unableToDisplayAccount.3", List.empty),
          ("sa.message.unableToDisplayAccount.4", List.empty)
        )

      val actualAccountSummary = SaAccountSummaryViewBuilder(mockPortalUrlBuilder.build _, mockUser, mockSaMicroService).build().get
      actualAccountSummary.regimeName shouldBe "Self Assessment (SA)"
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
    when(mockPortalUrlBuilder.build(SaAccountSummaryMessageKeys.makeAPaymentLink)).thenReturn(makeAPaymentUrl)

    val actualAccountSummary = SaAccountSummaryViewBuilder(mockPortalUrlBuilder.build _, mockUser, mockSaMicroService).build().get

    actualAccountSummary.regimeName shouldBe "Self Assessment (SA)"

    actualAccountSummary.messages shouldBe expectedMessages

    val expectedLinks = Seq(
      RenderableLinkMessage(LinkMessage(homeUrl, "sa.message.links.viewAccountDetails")),
      RenderableLinkMessage(LinkMessage(makeAPaymentUrl, SaAccountSummaryMessageKeys.makeAPaymentLink)),
      RenderableLinkMessage(LinkMessage(homeUrl, "sa.message.links.fileAReturn"))

    )
    actualAccountSummary.addenda shouldBe expectedLinks
  }

}