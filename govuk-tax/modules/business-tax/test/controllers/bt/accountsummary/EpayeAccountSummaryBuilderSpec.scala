package controllers.bt.accountsummary

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import controllers.bt.accountsummary.EpayeMessageKeys._
import controllers.bt.accountsummary.EpayePortalUrlKeys._
import uk.gov.hmrc.common.microservice.epaye.EpayeConnector
import controllers.bt.routes
import views.helpers.MoneyPounds
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.common.microservice.auth.domain.Authority
import scala.util.{Failure, Try, Success}
import CommonBusinessMessageKeys._
import uk.gov.hmrc.common.microservice.epaye.domain.{EpayeRoot, NonRTI, EpayeAccountSummary, RTI}
import controllers.common.actions.HeaderCarrier
import scala.concurrent._

trait DummyPortalUrlBuilder {
  def build(a: String): String
}


class EpayeAccountSummaryBuilderSpec extends BaseSpec with MockitoSugar {

  private val dummyEmpRef = EmpRef("abc", "defg")
  private val homeUrl = "http://homeUrl"
  private val makeAPaymentUrl = routes.PaymentController.makeEpayePayment().url
  private val empRefMessageString: Msg = Msg(epayeEmpRefMessage, Seq(dummyEmpRef.toString))

  private val expectedRtiLinks = Seq(
    AccountSummaryLink("epaye-account-details-href", homeUrl, viewAccountDetailsLinkMessage, sso = true),
    AccountSummaryLink("epaye-make-payment-href", makeAPaymentUrl, makeAPaymentLinkMessage, sso = false)
  )

  private val expectedNonRtiLinks = Seq(
    AccountSummaryLink("epaye-account-details-href", homeUrl, viewAccountDetailsLinkMessage, sso = true),
    AccountSummaryLink("epaye-make-payment-href", makeAPaymentUrl, makeAPaymentLinkMessage, sso = false)
  )

  "EpayeAccountSummaryViewBuilder with RTI" should {

    "build correct account summary model when amount due = amount paid to date" in {

      val rti = RTI(BigDecimal(0))
      val accountSummary = EpayeAccountSummary(rti = Some(rti))

      val expectedMessages = Seq(empRefMessageString, Msg(epayeNothingToPayMessage, Seq.empty))

      testEpayeAccountSummaryBuilder(epayeRegimeNameMessage, Success(Some(accountSummary)), expectedMessages, expectedRtiLinks)
    }

    "build correct account summary model when amount due < amount paid to date (negative balance)" in {
      val rti = RTI(BigDecimal(-8))
      val accountSummary = EpayeAccountSummary(rti = Some(rti))

      val overPaidMessage = Msg(epayeYouHaveOverpaidMessage, Seq(MoneyPounds(BigDecimal(8))))
      val adjustFuturePaymentsMessageString = Msg(epayeAdjustFuturePaymentsMessage)

      val expectedMessages = Seq[Msg](empRefMessageString, overPaidMessage, adjustFuturePaymentsMessageString)

      testEpayeAccountSummaryBuilder(epayeRegimeNameMessage, Success(Some(accountSummary)), expectedMessages, expectedRtiLinks)
    }

    "build correct account summary model when amount due > amount paid to date (positive balance)" in {
      val rti = RTI(BigDecimal(8))
      val accountSummary = EpayeAccountSummary(rti = Some(rti))

      val dueForPaymentMessageString = Msg(epayeDueForPaymentMessage, Seq(MoneyPounds(BigDecimal(8))))

      val expectedMessages = Seq(empRefMessageString, dueForPaymentMessageString)

      testEpayeAccountSummaryBuilder(epayeRegimeNameMessage, Success(Some(accountSummary)), expectedMessages, expectedRtiLinks)
    }
  }

  "EpayeAccountSummaryViewBuilder with Non-RTI" should {

    "populate the account summary model correctly if the amount paid to date is 0" in {
      val amountDue = BigDecimal(0)
      val nonRti = NonRTI(amountDue, 2013)
      val accountSummary = EpayeAccountSummary(nonRti = Some(nonRti))

      val paidToDateForPeriodMessageString = Msg(epayePaidToDateForPeriodMessage, Seq(MoneyPounds(BigDecimal(0)), "2013 - 14"))

      val expectedMessages = Seq(empRefMessageString, paidToDateForPeriodMessageString)

      testEpayeAccountSummaryBuilder(epayeRegimeNameMessage, Success(Some(accountSummary)), expectedMessages, expectedNonRtiLinks)
    }

    "populate account summary model correctly if the amount paid to date is > 0" in {
      val amountDue = BigDecimal(100)
      val nonRti = NonRTI(amountDue, 2011)
      val accountSummary = EpayeAccountSummary(nonRti = Some(nonRti))

      val paidToDateForPeriodMessageString = Msg(epayePaidToDateForPeriodMessage, Seq(MoneyPounds(BigDecimal(100)), "2011 - 12"))

      val expectedMessages = Seq(empRefMessageString, paidToDateForPeriodMessageString)

      testEpayeAccountSummaryBuilder(epayeRegimeNameMessage, Success(Some(accountSummary)), expectedMessages, expectedNonRtiLinks)
    }

    "populate account summary model correctly if the amount paid to is > 0 and date is 1999" in {
      val amountDue = BigDecimal(100)
      val nonRti = NonRTI(amountDue, 1999)
      val accountSummary = EpayeAccountSummary(nonRti = Some(nonRti))

      val paidToDateForPeriodMessageString = Msg(epayePaidToDateForPeriodMessage, Seq(MoneyPounds(BigDecimal(100)), "1999 - 00"))

      val expectedMessages = Seq(empRefMessageString, paidToDateForPeriodMessageString)

      testEpayeAccountSummaryBuilder(epayeRegimeNameMessage, Success(Some(accountSummary)), expectedMessages, expectedNonRtiLinks)
    }
  }

  "EpayeAccountSummaryViewBuilder" should {

    "build the correct account summary model when there is no RTI and Non-RTI account summary data" in {
      val accountSummary = EpayeAccountSummary()

      val unableToDisplayAccountInfoMessages = Seq(Msg(epayeSummaryUnavailableErrorMessage1),
        Msg(epayeSummaryUnavailableErrorMessage2),
        Msg(epayeSummaryUnavailableErrorMessage3),
        Msg(epayeSummaryUnavailableErrorMessage4))

      val expectedMessages = empRefMessageString +: unableToDisplayAccountInfoMessages
      testEpayeAccountSummaryBuilder("epaye.regimeName", Success(Some(accountSummary)), expectedMessages, Seq.empty)
    }

    "build the correct account summary model if no summary is returned from the service (e.g. due to 404 or 500 from the REST call)" in {

      val unableToDisplayAccountInfoMessages = Seq(Msg(epayeSummaryUnavailableErrorMessage1),
        Msg(epayeSummaryUnavailableErrorMessage2),
        Msg(epayeSummaryUnavailableErrorMessage3),
        Msg(epayeSummaryUnavailableErrorMessage4))
      val expectedMessages = empRefMessageString +: unableToDisplayAccountInfoMessages
      testEpayeAccountSummaryBuilder("epaye.regimeName", Success(None), expectedMessages, Seq.empty)
    }

    //    "return the oops summary if there is an exception when requesting the root" in {
    //      val empRef = EmpRef("ABC", "12345")
    //      val userAuthorityWithEmpRef = UserAuthority("123", Regimes(), empRef = Some(empRef))
    //      val regimeRoots = RegimeRoots(epaye = Some(new NumberFormatException)))
    //      val user = User("tim", userAuthorityWithEmpRef, regimeRoots, None, None)
    //      val mockEpayeConnector = mock[EpayeConnector]
    //      val builder = new EpayeAccountSummaryBuilder(mockEpayeConnector)
    //      val accountSummaryOption: Option[AccountSummary] = builder.build(buildPortalUrl, user)
    //      accountSummaryOption should not be None
    //      val accountSummary = accountSummaryOption.get
    //      accountSummary.regimeName shouldBe epayeUnknownRegimeName
    //      accountSummary.messages shouldBe Seq[Msg](Msg(oopsMessage , Seq.empty))
    //      accountSummary.addenda shouldBe Seq.empty
    //      accountSummary.status shouldBe SummaryStatus.oops
    //      verifyZeroInteractions(mockEpayeConnector)
    //    }

    "return the oops summary if there is an exception when requesting the account summary" in {
      val expectedMessages = Seq(Msg(oopsMessage, Seq.empty))
      testEpayeAccountSummaryBuilder(epayeUnknownRegimeName, Failure(new NumberFormatException), expectedMessages, Seq.empty)
    }
  }

  private def testEpayeAccountSummaryBuilder(expectedRegimeName: String, accountSummary: Try[Option[EpayeAccountSummary]], expectedMessages: Seq[Msg], expectedLinks: Seq[AccountSummaryLink]) {
    val mockUser = mock[User]
    val mockUserAuthority = mock[Authority]
    val mockEpayeConnector = mock[EpayeConnector]
    val mockRegimeRoots = mock[RegimeRoots]
    val mockEpayeRoot = mock[EpayeRoot]

    val mockPortalUrlBuilder = mock[DummyPortalUrlBuilder]

    implicit val hc = HeaderCarrier()

    when(mockUser.regimes).thenReturn(mockRegimeRoots)
    when(mockUser.userAuthority).thenReturn(mockUserAuthority)
    when(mockRegimeRoots.epaye).thenReturn(Some(mockEpayeRoot))
    when(mockEpayeRoot.identifier).thenReturn(dummyEmpRef)
    accountSummary match {
      case Success(aSummary) => when(mockEpayeRoot.accountSummary(mockEpayeConnector, hc)).thenReturn(Future.successful(aSummary))
      case Failure(exception) => when(mockEpayeRoot.accountSummary(mockEpayeConnector, hc)).thenThrow(exception)
    }

    when(mockPortalUrlBuilder.build(epayeHomePortalUrl)).thenReturn(homeUrl)
    when(mockPortalUrlBuilder.build(makeAPaymentLinkMessage)).thenReturn(makeAPaymentUrl) // TODO [JJS] THIS ISN'T A PORTAL LINK IS IT? AND WE'RE PASSING A MESSAGE TO THE LINK BUILDER? - THIS LINE LOOKS WRONG

    val actualAccountSummary = await(EpayeAccountSummaryBuilder(mockEpayeConnector).build(mockPortalUrlBuilder.build _, mockUser).get)

    actualAccountSummary.regimeName shouldBe expectedRegimeName
    actualAccountSummary.messages shouldBe expectedMessages
    actualAccountSummary.addenda shouldBe expectedLinks
  }
}
