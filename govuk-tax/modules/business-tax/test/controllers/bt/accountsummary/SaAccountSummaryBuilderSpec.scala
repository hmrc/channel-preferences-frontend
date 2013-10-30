package controllers.bt.accountsummary

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.sa.SaConnector
import org.mockito.Mockito._
import views.helpers._
import org.joda.time.LocalDate
import controllers.bt.routes
import controllers.bt.accountsummary.SaMessageKeys._
import views.helpers.LinkMessage
import views.helpers.RenderableLinkMessage
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import SaPortalUrlKeys._
import uk.gov.hmrc.common.microservice.auth.domain.{Regimes, UserAuthority}
import uk.gov.hmrc.domain.SaUtr
import CommonBusinessMessageKeys._
import uk.gov.hmrc.common.microservice.sa.domain.{SaRoot, Liability, AmountDue, SaAccountSummary}

class SaAccountSummaryBuilderSpec extends BaseSpec with MockitoSugar {
  private val homeUrl = "http://home"
  private val makeAPaymentUrl = routes.BusinessTaxController.makeAPaymentLanding().url
  private val liabilityDate = new LocalDate(2014, 1, 15)
  private val saUtr = SaUtr("123456789")
  private val userAuthorityWithSa = UserAuthority("123", Regimes(), None, Some(saUtr))
  private val buildPortalUrl: (String) => String = (value: String) => value
  private val utrMessage = Msg("sa.message.utr", Seq(saUtr.utr))


  "Sa Account SummaryView Builder builds correct Account Summary model " should {
    " when no amounts are due now or later " in {
      val totalAmountDueToHmrc = None
      val nextPayment = None
      val amountHmrcOwe = None

      val accountSummary = SaAccountSummary(totalAmountDueToHmrc, nextPayment, amountHmrcOwe)
      val expectedMessages = Seq(Msg(saNothingToPayMessage, Seq.empty))

      testSaAccountSummaryBuilder(accountSummary, expectedMessages)
    }

    "when an amount is due now (payable) " in {
      val amountDue = BigDecimal(100)
      val totalAmountDueToHmrc = Some(AmountDue(amountDue, true))
      val nextPayment = None
      val amountHmrcOwe = Some(BigDecimal(0))

      val accountSummary = SaAccountSummary(totalAmountDueToHmrc, nextPayment, amountHmrcOwe)
      val expectedMessages = Seq(Msg(saAmountDueForPaymentMessage, Seq(MoneyPounds(amountDue))), Msg(saInterestApplicableMessage, Seq.empty))

      testSaAccountSummaryBuilder(accountSummary, expectedMessages)

    }

    "when amounts are due now and later " in {
      val amountDue = BigDecimal(100)
      val totalAmountDueToHmrc = Some(AmountDue(amountDue, true))

      val liabilityAmount = BigDecimal(20)
      val nextPayment = Some(Liability(new LocalDate(2014, 1, 15), liabilityAmount))
      val amountHmrcOwe = Some(BigDecimal(0))

      val accountSummary = SaAccountSummary(totalAmountDueToHmrc, nextPayment, amountHmrcOwe)
      val expectedMessages = Seq(Msg(saAmountDueForPaymentMessage, Seq(MoneyPounds(amountDue))), Msg(saInterestApplicableMessage),
        Msg(saWillBecomeDueMessage, Seq(MoneyPounds(liabilityAmount), liabilityDate)))

      testSaAccountSummaryBuilder(accountSummary, expectedMessages)

    }

    "when an amount is due for repayment to the user " in {
      val totalAmountDueToHmrc = None
      val nextPayment = None
      val amountHmrcOwe = Some(BigDecimal(100))

      val accountSummary = SaAccountSummary(totalAmountDueToHmrc, nextPayment, amountHmrcOwe)
      val expectedMessages = Seq[Msg](
        Msg(saYouHaveOverpaidMessage),
        Msg(saAmountDueForRepaymentMessage, Seq(MoneyPounds(amountHmrcOwe.get))))

      testSaAccountSummaryBuilder(accountSummary, expectedMessages)

    }

    "when an amount is due for repayment to the user and an amount is becoming due for repayment " in {
      val totalAmountDueToHmrc = None
      val liabilityAmount = BigDecimal(20)
      val nextPayment = Some(Liability(new LocalDate(2014, 1, 15), liabilityAmount))
      val amountHmrcOwe = Some(BigDecimal(100))

      val accountSummary = SaAccountSummary(totalAmountDueToHmrc, nextPayment, amountHmrcOwe)
      val expectedMessages = Seq[Msg](
        Msg(saYouHaveOverpaidMessage), Msg(saAmountDueForRepaymentMessage, Seq(MoneyPounds(amountHmrcOwe.get))),
        Msg(saWillBecomeDueMessage, Seq(MoneyPounds(liabilityAmount), liabilityDate)))
      testSaAccountSummaryBuilder(accountSummary, expectedMessages)
    }

    "when no amounts due currently but amounts becoming due for payment " in {
      val totalAmountDueToHmrc = Some(AmountDue(BigDecimal(0), false))
      val liabilityAmount = BigDecimal(20)
      val nextPayment = Some(Liability(new LocalDate(2014, 1, 15), liabilityAmount))
      val amountHmrcOwe = Some(BigDecimal(0))

      val accountSummary = SaAccountSummary(totalAmountDueToHmrc, nextPayment, amountHmrcOwe)
      val expectedMessages = Seq[Msg](
        Msg(saNothingToPayMessage, Seq.empty),
        Msg(saWillBecomeDueMessage, Seq(MoneyPounds(liabilityAmount), liabilityDate))
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
        Msg(saAmountDueForPaymentMessage, Seq(MoneyPounds(amountDue))),
        Msg(saSmallAmountToPayMessage),
        Msg(saWillBecomeDueMessage, Seq(MoneyPounds(liabilityAmount), liabilityDate))
      )
      testSaAccountSummaryBuilder(accountSummary, expectedMessages)
    }

    "when account summary is not available " in {
      val mockUser = mock[User]
      val mockSaConnector = mock[SaConnector]
      val mockRegimeRoots = mock[RegimeRoots]
      val mockSaRoot = mock[SaRoot]

      val mockPortalUrlBuilder = mock[DummyPortalUrlBuilder]

      when(mockUser.regimes).thenReturn(mockRegimeRoots)
      when(mockRegimeRoots.sa).thenReturn(Some(mockSaRoot))
      when(mockSaRoot.accountSummary(mockSaConnector)).thenReturn(None)
      when(mockSaRoot.identifier).thenReturn(saUtr)

      val expectedMessages =
        utrMessage +: Seq(
          Msg(saSummaryUnavailableErrorMessage1),
          Msg(saSummaryUnavailableErrorMessage2),
          Msg(saSummaryUnavailableErrorMessage3),
          Msg(saSummaryUnavailableErrorMessage4)
        )

      val actualAccountSummary = SaAccountSummaryBuilder(mockSaConnector).build(mockPortalUrlBuilder.build, mockUser).get
      actualAccountSummary.regimeName shouldBe saRegimeName
      actualAccountSummary.messages shouldBe expectedMessages

    }
//    "return the oops summary if there is an exception when requesting the root" in {
//      val regimeRoots = RegimeRoots(sa = Some(Failure(new NumberFormatException)))
//      val user = User("tim", userAuthorityWithSa, regimeRoots, None, None)
//      val mockSaConnector = mock[SaConnector]
//      val builder = new SaAccountSummaryBuilder(mockSaConnector)
//      val accountSummaryOption: Option[AccountSummary] = builder.build(buildPortalUrl, user)
//      accountSummaryOption should not be None
//      val accountSummary = accountSummaryOption.get
//      accountSummary.regimeName shouldBe saRegimeName
//      accountSummary.messages shouldBe Seq[Msg](Msg(oopsMessage, Seq.empty))
//      accountSummary.addenda shouldBe Seq.empty
//      accountSummary.status shouldBe SummaryStatus.oops
//      verifyZeroInteractions(mockSaConnector)
//    }

//    "return the oops summary if there is an exception when requesting the account summary" in {
//      val regimeRoots = RegimeRoots(sa = Some(Success(SaRoot(saUtr, Map("individual/account-summary" -> s"/sa/$saUtr/account-summary")))))
//      val user = User("tim", userAuthorityWithSa, regimeRoots, None, None)
//      val mockSaConnector = mock[SaConnector]
//      when(mockSaConnector.accountSummary(s"/sa/$saUtr/account-summary")).thenThrow(new NumberFormatException)
//      val builder = new SaAccountSummaryBuilder(mockSaConnector)
//      val accountSummaryOption: Option[AccountSummary] = builder.build(buildPortalUrl, user)
//      accountSummaryOption should not be None
//      val accountSummary = accountSummaryOption.get
//      accountSummary.regimeName shouldBe saRegimeName
//      accountSummary.messages shouldBe Seq[Msg](Msg(oopsMessage, Seq.empty))
//      accountSummary.addenda shouldBe Seq.empty
//      accountSummary.status shouldBe SummaryStatus.oops
//    }


  }

  trait DummyPortalUrlBuilder {
    def build(a: String): String
  }


  private def testSaAccountSummaryBuilder(accountSummary: SaAccountSummary, expectedMessages: Seq[Msg]) {
    val mockUser = mock[User]
    val mockSaConnector = mock[SaConnector]
    val mockRegimeRoots = mock[RegimeRoots]
    val mockSaRoot = mock[SaRoot]

    val mockPortalUrlBuilder = mock[DummyPortalUrlBuilder]

    when(mockUser.regimes).thenReturn(mockRegimeRoots)
    when(mockRegimeRoots.sa).thenReturn(Some(mockSaRoot))
    when(mockSaRoot.accountSummary(mockSaConnector)).thenReturn(Some(accountSummary))
    when(mockSaRoot.identifier).thenReturn(saUtr)

    when(mockPortalUrlBuilder.build(saHomePortalUrl)).thenReturn(homeUrl)
    when(mockPortalUrlBuilder.build(makeAPaymentLinkMessage)).thenReturn(makeAPaymentUrl)

    val actualAccountSummary = SaAccountSummaryBuilder(mockSaConnector).build(mockPortalUrlBuilder.build, mockUser).get

    actualAccountSummary.regimeName shouldBe SaMessageKeys.saRegimeName
    actualAccountSummary.messages shouldBe utrMessage +: expectedMessages

    val expectedLinks = Seq(
      RenderableLinkMessage(LinkMessage(homeUrl, viewAccountDetailsLinkMessage, Some("portalLink"), sso = true)),
      RenderableLinkMessage(LinkMessage(makeAPaymentUrl, makeAPaymentLinkMessage, sso = false)),
      RenderableLinkMessage(LinkMessage(homeUrl, fileAReturnLinkMessage, sso = true))

    )
    actualAccountSummary.addenda shouldBe expectedLinks
  }

}