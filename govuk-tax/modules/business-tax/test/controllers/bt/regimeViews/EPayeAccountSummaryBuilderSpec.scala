package controllers.bt.regimeViews

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import views.helpers.RenderableMessage
import org.mockito.Mockito._
import controllers.bt.regimeViews.EPayeMessageKeys._
import controllers.bt.regimeViews.EPayePortalUrlKeys._
import uk.gov.hmrc.common.microservice.epaye.domain.EPayeDomain._
import uk.gov.hmrc.common.microservice.epaye.EPayeConnector
import controllers.bt.routes
import views.helpers.LinkMessage
import uk.gov.hmrc.common.microservice.epaye.domain.EPayeDomain.EPayeRoot
import views.helpers.RenderableLinkMessage
import views.helpers.MoneyPounds
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import uk.gov.hmrc.common.microservice.epaye.domain.EPayeDomain.RTI
import uk.gov.hmrc.common.microservice.epaye.domain.EPayeDomain.EPayeAccountSummary
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority

trait DummyPortalUrlBuilder {
  def build(a: String): String
}

class EPayeAccountSummaryBuilderSpec extends BaseSpec with MockitoSugar {

  private val dummyEmpRef = EmpRef("abc", "defg")
  private val homeUrl = "http://homeUrl"
  private val makeAPaymentUrl = routes.BusinessTaxController.makeAPaymentLanding().url
  private val empRefMessageString : (String, Seq[RenderableMessage]) = (epayeEmpRefMessage, Seq(dummyEmpRef.toString))

  "EPayeAccountSummaryViewBuilder with RTI" should {
    
    "build correct account summary model when amount due = amount paid to date" in {

      val rti = RTI(BigDecimal(0))
      val accountSummary = EPayeAccountSummary(rti = Some(rti))

      val expectedMessages = Seq[(String, Seq[RenderableMessage])](
        empRefMessageString,
        (epayeNothingToPayMessage, Seq.empty)
      )

      testEPayeAccountSummaryBuilder(epayeRtiRegimeNameMessage, Some(accountSummary), expectedMessages)
    }

    "build correct account summary model when amount due < amount paid to date (negative balance)" in {
      val rti = RTI(BigDecimal(-8))
      val accountSummary = EPayeAccountSummary(rti = Some(rti))

      val overPaidMessage = (epayeYouHaveOverpaidMessage, Seq[RenderableMessage](MoneyPounds(BigDecimal(8))))
      val adjustFuturePaymentsMessageString = (epayeAdjustFuturePaymentsMessage, Seq.empty)

      val expectedMessages = Seq[(String, Seq[RenderableMessage])](
        empRefMessageString, overPaidMessage, adjustFuturePaymentsMessageString
      )

      testEPayeAccountSummaryBuilder(epayeRtiRegimeNameMessage, Some(accountSummary), expectedMessages)
    }

    "build correct account summary model when amount due > amount paid to date (positive balance)" in {
      val rti = RTI(BigDecimal(8))
      val accountSummary = EPayeAccountSummary(rti = Some(rti))

      val dueForPaymentMessageString = (epayeDueForPaymentMessage, Seq[RenderableMessage](MoneyPounds(BigDecimal(8))))

      val expectedMessages = Seq[(String, Seq[RenderableMessage])](empRefMessageString, dueForPaymentMessageString)

      testEPayeAccountSummaryBuilder(epayeRtiRegimeNameMessage, Some(accountSummary), expectedMessages)
    }
  }

  "EPayeAccountSummaryViewBuilder with Non-RTI" should {
    
    "populate the account summary model correctly if the amount paid to date is 0" in {
      val amountDue = BigDecimal(0)
      val nonRti = NonRTI(amountDue, 2013)
      val accountSummary = EPayeAccountSummary(nonRti = Some(nonRti))

      val paidToDateForPeriodMessageString = (epayePaidToDateForPeriodMessage, Seq[RenderableMessage](MoneyPounds(BigDecimal(0)), "2013 - 14"))

      val expectedMessages = Seq[(String, Seq[RenderableMessage])](empRefMessageString, paidToDateForPeriodMessageString)

      testEPayeAccountSummaryBuilder(epayeNonRtiRegimeNameMessage, Some(accountSummary), expectedMessages)
    }

    "populate account summary model correctly if the amount paid to date is > 0" in {
      val amountDue = BigDecimal(100)
      val nonRti = NonRTI(amountDue, 2011)
      val accountSummary = EPayeAccountSummary(nonRti = Some(nonRti))

      val paidToDateForPeriodMessageString = (epayePaidToDateForPeriodMessage, Seq[RenderableMessage](MoneyPounds(BigDecimal(100)), "2011 - 12"))

      val expectedMessages = Seq[(String, Seq[RenderableMessage])](empRefMessageString, paidToDateForPeriodMessageString)

      testEPayeAccountSummaryBuilder(epayeNonRtiRegimeNameMessage, Some(accountSummary), expectedMessages)
    }

    "populate account summary model correctly if the amount paid to is > 0 and date is 1999" in {
      val amountDue = BigDecimal(100)
      val nonRti = NonRTI(amountDue, 1999)
      val accountSummary = EPayeAccountSummary(nonRti = Some(nonRti))

      val paidToDateForPeriodMessageString = (epayePaidToDateForPeriodMessage, Seq[RenderableMessage](MoneyPounds(BigDecimal(100)), "1999 - 00"))

      val expectedMessages = Seq[(String, Seq[RenderableMessage])](empRefMessageString, paidToDateForPeriodMessageString)

      testEPayeAccountSummaryBuilder(epayeNonRtiRegimeNameMessage, Some(accountSummary), expectedMessages)
    }
  }

  "EPayeAccountSummaryViewBuilder" should {

    "build the correct account summary model when there is no RTI and Non-RTI account summary data" in {
      val accountSummary = EPayeAccountSummary()

      val unableToDisplayAccountInfoMessage = (epayeSummaryUnavailableErrorMessage, Seq.empty)
      val expectedMessages = Seq[(String, Seq[RenderableMessage])](empRefMessageString, unableToDisplayAccountInfoMessage)

      testEPayeAccountSummaryBuilder("epaye.regimeName.unknown", Some(accountSummary), expectedMessages)
    }

    "build the correct account summary model if no summary is returned from the service (e.g. due to 404 or 500 from the REST call)" in {

      val unableToDisplayAccountInfoMessage = (epayeSummaryUnavailableErrorMessage, Seq.empty)
      val expectedMessages = Seq[(String, Seq[RenderableMessage])](empRefMessageString, unableToDisplayAccountInfoMessage)
      testEPayeAccountSummaryBuilder("epaye.regimeName.unknown", None, expectedMessages)
    }
  }

  private def testEPayeAccountSummaryBuilder(expectedRegimeName: String, accountSummary: Option[EPayeAccountSummary], expectedMessages: Seq[(String, Seq[RenderableMessage])]) {
    val mockUser = mock[User]
    val mockUserAuthority = mock[UserAuthority]
    val mockEPayeConnector = mock[EPayeConnector]
    val mockRegimeRoots = mock[RegimeRoots]
    val mockEPayeRoot = mock[EPayeRoot]

    val mockPortalUrlBuilder = mock[DummyPortalUrlBuilder]

    when(mockUser.regimes).thenReturn(mockRegimeRoots)
    when(mockUser.userAuthority).thenReturn(mockUserAuthority)
    when(mockRegimeRoots.epaye).thenReturn(Some(mockEPayeRoot))
    when(mockEPayeRoot.identifier).thenReturn(dummyEmpRef)
    when(mockEPayeRoot.accountSummary(mockEPayeConnector)).thenReturn(accountSummary)

    when(mockPortalUrlBuilder.build(epayeHomePortalUrl)).thenReturn(homeUrl)
    when(mockPortalUrlBuilder.build(makeAPaymentLinkMessage)).thenReturn(makeAPaymentUrl) // TODO [JJS] THIS ISN'T A PORTAL LINK IS IT? AND WE'RE PASSING A MESSAGE TO THE LINK BUILDER? - THIS LINE LOOKS WRONG

    val actualAccountSummary = EPayeAccountSummaryBuilder(mockEPayeConnector).build(mockPortalUrlBuilder.build _, mockUser).get

    actualAccountSummary.regimeName shouldBe expectedRegimeName

    actualAccountSummary.messages shouldBe expectedMessages

    val expectedLinks = Seq(
      RenderableLinkMessage(LinkMessage(homeUrl, viewAccountDetailsLinkMessage)),
      RenderableLinkMessage(LinkMessage(makeAPaymentUrl, makeAPaymentLinkMessage)),
      RenderableLinkMessage(LinkMessage(homeUrl, fileAReturnLinkMessage))

    )
    actualAccountSummary.addenda shouldBe expectedLinks
  }
}
