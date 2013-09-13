package controllers.bt.regimeViews

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.microservice.sa.domain.{Liability, SaRoot, AmountDue, SaAccountSummary}
import uk.gov.hmrc.microservice.sa.SaMicroService
import uk.gov.hmrc.microservice.domain.{RegimeRoots, User}
import org.mockito.Mockito._
import views.helpers.{RenderableLinkMessage, RenderableStringMessage, RenderableMessage, LinkMessage}
import org.joda.time.LocalDate

class SaAccountSummaryViewBuilderSpec extends BaseSpec with MockitoSugar {

  trait DummyPortalUrlBuilder {
    def build(a: String): String
  }

  val viewAccountDetailsUrl = "http://viewAccountDetails"
  val fileAReturnUrl = "http://fileAReturn"

  "Sa Account SummaryView Builder builds correct Account Summary model " should {
    " when no amounts are due now or later " in {
      val totalAmountDueToHmrc = None
      val nextPayment = None
      val amountHmrcOwe = None

      val accountSummary = SaAccountSummary(totalAmountDueToHmrc, nextPayment, amountHmrcOwe)
      val expectedMessages: Seq[(String, List[RenderableMessage])] =
        Seq(
          ("sa.message.nothing-to-pay", List.empty),
          ("sa.message.view-history", List.empty)
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
        ("sa.message.amount-due-for-payment", List(RenderableStringMessage(amountDue.toString()))),
        ("sa.message.interest-applicable", List.empty)
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
        ("sa.message.amount-due-for-payment", List(RenderableStringMessage(amountDue.toString()))),
        ("sa.message.interest-applicable", List.empty),
        ("sa.message.will-become-due", List(RenderableStringMessage(liabilityAmount.toString()), RenderableStringMessage("2014-01-15")))
      )

      testSaAccountSummaryBuilder(accountSummary, expectedMessages)

    }

    "when an amount is due for repayment to the user " in {
      val totalAmountDueToHmrc = None
      val nextPayment = None
      val amountHmrcOwe = Some(BigDecimal(100))

      val accountSummary = SaAccountSummary(totalAmountDueToHmrc, nextPayment, amountHmrcOwe)
      val expectedMessages = Seq(
        ("sa.message.you-have-overpaid", List.empty),
        ("sa.message.amount-due-for-repayment", List(RenderableStringMessage(amountHmrcOwe.get.toString()))),
        ("sa.message.view-history", List.empty)
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
        ("sa.message.you-have-overpaid", List.empty),
        ("sa.message.amount-due-for-repayment", List(RenderableStringMessage(amountHmrcOwe.get.toString()))),
        ("sa.message.will-become-due", List(RenderableStringMessage(liabilityAmount.toString()), RenderableStringMessage("2014-01-15")))
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
        ("sa.message.nothing-to-pay", List.empty),
        ("sa.message.will-become-due", List(RenderableStringMessage(liabilityAmount.toString()), RenderableStringMessage("2014-01-15")))
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
        ("sa.message.amount-due-for-payment", List(RenderableStringMessage(amountDue.toString()))),
        ("sa.message.small-amount-to-pay", List.empty),
        ("sa.message.will-become-due", List(RenderableStringMessage(liabilityAmount.toString()), RenderableStringMessage("2014-01-15")))
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

      val expectedMessages = Seq(("sa.message.unable-to-display-account", List.empty))

      val actualAccountSummary = SaAccountSummaryViewBuilder(mockPortalUrlBuilder.build _, mockUser, mockSaMicroService).build().get
      actualAccountSummary.regimeName shouldBe "SA"
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

    when(mockPortalUrlBuilder.build("saViewAccountDetails")).thenReturn(viewAccountDetailsUrl)
    when(mockPortalUrlBuilder.build("saFileAReturn")).thenReturn(fileAReturnUrl)

    val actualAccountSummary = SaAccountSummaryViewBuilder(mockPortalUrlBuilder.build _, mockUser, mockSaMicroService).build().get

    actualAccountSummary.regimeName shouldBe "SA"

    actualAccountSummary.messages shouldBe expectedMessages

    val expectedLinks = Seq(RenderableLinkMessage(LinkMessage(viewAccountDetailsUrl, "sa.message.links.view-account-details")), RenderableLinkMessage(LinkMessage(fileAReturnUrl, "sa.message.links.file-a-return")))
    actualAccountSummary.addenda shouldBe expectedLinks
  }

}


