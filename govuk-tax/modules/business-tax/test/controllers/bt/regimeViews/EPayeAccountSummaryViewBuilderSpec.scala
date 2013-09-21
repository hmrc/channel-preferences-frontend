package controllers.bt.regimeViews

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import views.helpers.RenderableMessage
import org.mockito.Mockito._
import controllers.bt.regimeViews.EPayeAccountSummaryMessageKeys._
import uk.gov.hmrc.common.microservice.epaye.domain.EPayeDomain._
import uk.gov.hmrc.common.microservice.epaye.EPayeMicroService
import controllers.bt.routes
import views.helpers.LinkMessage
import uk.gov.hmrc.common.microservice.epaye.domain.EPayeDomain.EPayeRoot
import views.helpers.RenderableLinkMessage
import scala.Some
import views.helpers.MoneyPounds
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import uk.gov.hmrc.common.microservice.epaye.domain.EPayeDomain.AmountDue
import uk.gov.hmrc.common.microservice.epaye.domain.EPayeDomain.RTI
import uk.gov.hmrc.common.microservice.epaye.domain.EPayeDomain.EPayeAccountSummary
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority

trait DummyPortalUrlBuilder {
  def build(a: String): String
}

class EPayeAccountSummaryViewBuilderSpec extends BaseSpec with MockitoSugar {

  val dummyEmpRef = EmpRef("abc", "defg")
  val fileAReturnUrl = "http://fileAReturn"
  val homeUrl = "http://homeUrl"
  val makeAPaymentUrl = routes.BusinessTaxController.makeAPaymentLanding().url
  val empRefMessage : (String, Seq[RenderableMessage]) = (empRef, Seq(dummyEmpRef.toString))

  "ePaye Account SummaryView Builder with RTI" should {
    "build correct Account Summary model when amount due = amount paid to date" in {

      val rti = RTI(BigDecimal(0))
      val accountSummary = EPayeAccountSummary(rti = Some(rti))

      val expectedMessages = Seq[(String, Seq[RenderableMessage])](
        empRefMessage,
        (nothingToPay, Seq.empty)
      )

      testSaAccountSummaryBuilder(accountSummary, expectedMessages)
    }

    "build correct Account Summary model when amount due < amount paid to date (negative balance)" in {
      val rti = RTI(BigDecimal(-8))
      val accountSummary = EPayeAccountSummary(rti = Some(rti))

      val overPaidMessage = (youHaveOverpaid, Seq[RenderableMessage](MoneyPounds(BigDecimal(8))))
      val adjustFuturePaymentsMessage = (adjustFuturePayments, Seq.empty)

      val expectedMessages = Seq[(String, Seq[RenderableMessage])](
        empRefMessage, overPaidMessage, adjustFuturePaymentsMessage
      )

      testSaAccountSummaryBuilder(accountSummary, expectedMessages)
    }

    "build correct Account Summary model when amount due > amount paid to date (positive balance)" in {
      val rti = RTI(BigDecimal(8))
      val accountSummary = EPayeAccountSummary(rti = Some(rti))

      val dueForPaymentMessage = (dueForPayment, Seq[RenderableMessage](MoneyPounds(BigDecimal(8))))

      val expectedMessages = Seq[(String, Seq[RenderableMessage])](empRefMessage, dueForPaymentMessage)

      testSaAccountSummaryBuilder(accountSummary, expectedMessages)
    }

    "build correct Account Summary model when no RTI and Non-RTI account summary data" in {
      val accountSummary = EPayeAccountSummary()

      val unableToDisplayAccountInfoMessage = (unableToDisplayAccountInformation, Seq.empty)
      val expectedMessages = Seq[(String, Seq[RenderableMessage])](empRefMessage, unableToDisplayAccountInfoMessage)

      testSaAccountSummaryBuilder(accountSummary, expectedMessages)
    }
  }

  "ePaye Account SummaryView Builder builds correct Account Summary model with Non-RTI" should {
    "populate account summary model correctly if the amount paid to date is 0" in {
      val amountDue = AmountDue(BigDecimal(0))
      val nonRti = NonRTI(amountDue, 2013)
      val accountSummary = EPayeAccountSummary(nonRti = Some(nonRti))

      val paidToDateForPeriodMessage = (paidToDateForPeriod, Seq[RenderableMessage](MoneyPounds(BigDecimal(0)), "2013 - 14"))

      val expectedMessages = Seq[(String, Seq[RenderableMessage])](empRefMessage, paidToDateForPeriodMessage)

      testSaAccountSummaryBuilder(accountSummary, expectedMessages)
    }

    "populate account summary model correctly if the amount paid to date is > 0" in {
      val amountDue = AmountDue(BigDecimal(100))
      val nonRti = NonRTI(amountDue, 2011)
      val accountSummary = EPayeAccountSummary(nonRti = Some(nonRti))

      val paidToDateForPeriodMessage = (paidToDateForPeriod, Seq[RenderableMessage](MoneyPounds(BigDecimal(100)), "2011 - 12"))

      val expectedMessages = Seq[(String, Seq[RenderableMessage])](empRefMessage, paidToDateForPeriodMessage)

      testSaAccountSummaryBuilder(accountSummary, expectedMessages)
    }

    "populate account summary model correctly if the amount paid to is > 0 and date is 1999" in {
      val amountDue = AmountDue(BigDecimal(100))
      val nonRti = NonRTI(amountDue, 1999)
      val accountSummary = EPayeAccountSummary(nonRti = Some(nonRti))

      val paidToDateForPeriodMessage = (paidToDateForPeriod, Seq[RenderableMessage](MoneyPounds(BigDecimal(100)), "1999 - 00"))

      val expectedMessages = Seq[(String, Seq[RenderableMessage])](empRefMessage, paidToDateForPeriodMessage)

      testSaAccountSummaryBuilder(accountSummary, expectedMessages)
    }
  }

  private def testSaAccountSummaryBuilder(accountSummary: EPayeAccountSummary, expectedMessages: Seq[(String, Seq[RenderableMessage])]) {
    val mockUser = mock[User]
    val mockUserAuthority = mock[UserAuthority]
    val mockEPayeMicroService = mock[EPayeMicroService]
    val mockRegimeRoots = mock[RegimeRoots]
    val mockEPayeRoot = mock[EPayeRoot]

    val mockPortalUrlBuilder = mock[DummyPortalUrlBuilder]

    when(mockUser.regimes).thenReturn(mockRegimeRoots)
    when(mockUser.userAuthority).thenReturn(mockUserAuthority)
    when(mockUserAuthority.empRef).thenReturn(Some(dummyEmpRef))
    when(mockRegimeRoots.epaye).thenReturn(Some(mockEPayeRoot))
    when(mockEPayeRoot.accountSummary(mockEPayeMicroService)).thenReturn(Some(accountSummary))

    when(mockPortalUrlBuilder.build("home")).thenReturn(homeUrl)
    when(mockPortalUrlBuilder.build(makeAPaymentLink)).thenReturn(makeAPaymentUrl)

    val actualAccountSummary = EPayeAccountSummaryViewBuilder(mockPortalUrlBuilder.build _, mockUser, mockEPayeMicroService).build().get

    actualAccountSummary.regimeName shouldBe "Employers PAYE (RTI)"

    actualAccountSummary.messages shouldBe expectedMessages

    val expectedLinks = Seq(
      RenderableLinkMessage(LinkMessage(homeUrl, viewAccountDetailsLink)),
      RenderableLinkMessage(LinkMessage(makeAPaymentUrl, makeAPaymentLink)),
      RenderableLinkMessage(LinkMessage(homeUrl, fileAReturnLink))

    )
    actualAccountSummary.addenda shouldBe expectedLinks
  }
}
