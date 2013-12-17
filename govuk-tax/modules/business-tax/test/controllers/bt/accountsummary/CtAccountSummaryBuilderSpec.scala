package controllers.bt.accountsummary

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.joda.time.LocalDate
import org.joda.time.chrono.ISOChronology
import CommonBusinessMessageKeys._
import CtMessageKeys._
import CtPortalUrlKeys._
import uk.gov.hmrc.common.microservice.ct.CtConnector
import controllers.bt.accountsummary.SummaryStatus._
import uk.gov.hmrc.common.microservice.ct.domain.CtRoot
import uk.gov.hmrc.common.microservice.vat.domain.VatRoot
import scala.concurrent.Future
import controllers.domain.AuthorityUtils._
import scala.Some
import uk.gov.hmrc.domain.Vrn
import views.helpers.MoneyPounds
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import uk.gov.hmrc.common.microservice.ct.domain.CtAccountSummary
import uk.gov.hmrc.common.microservice.ct.domain.CtAccountBalance

class CtAccountSummaryBuilderSpec extends BaseSpec with MockitoSugar {

  private val buildPortalUrl: (String) => String = (value: String) => value
  private val ctUtr = CtUtr("12347")

  private val regimeRootsWithCt = RegimeRoots(ct = Some(CtRoot(ctUtr, Map("accountSummary" -> s"/ct/${ctUtr.utr}/account-summary"))))
  private val userEnrolledForCt = User("tim",  ctAuthority("tim", "12347"), regimeRootsWithCt, None, None)

  private val vrn = Vrn("123")
  private val regimeRootsWithoutCt = RegimeRoots(vat = Some(VatRoot(vrn, Map("accountSummary" -> s"/vat/vrn/${vrn.vrn}"))))
  private val userNotEnrolledForCt = User("jim", saAuthority("jim", "123"), regimeRootsWithoutCt, None, None)

  "The CtAccountSummaryBuilder build method" should {

    "return the correct account summary for complete data" in {
      val ctConnectorMock = mock[CtConnector]
      val ctAccountSummary = CtAccountSummary(Some(CtAccountBalance(Some(4.2))), Some("2012-12-02"))
      when(ctConnectorMock.accountSummary(s"/ct/${ctUtr.utr}/account-summary")).thenReturn(Future.successful(Some(ctAccountSummary)))
      val builder = new CtAccountSummaryBuilder(ctConnectorMock)

      val accountSummaryOption: Option[Future[AccountSummary]] = builder.build(buildPortalUrl, userEnrolledForCt)
      accountSummaryOption should not be None
      val accountSummary = await(accountSummaryOption.get)
      accountSummary.regimeName shouldBe ctRegimeNameMessage
      accountSummary.messages shouldBe Seq[Msg](Msg(ctUtrMessage, Seq("12347")), Msg(ctAmountAsOfDateMessage, Seq(MoneyPounds(BigDecimal(4.2)), new LocalDate(2012, 12, 2, ISOChronology.getInstanceUTC))))
      accountSummary.addenda shouldBe Seq[AccountSummaryLink](
        AccountSummaryLink("ct-account-details-href", ctAccountDetailsPortalUrl, viewAccountDetailsLinkMessage, sso = true),
        AccountSummaryLink("ct-make-payment-href", "/ct/make-a-payment", makeAPaymentLinkMessage, sso = false),
        AccountSummaryLink("ct-file-return-href", ctFileAReturnPortalUrl, fileAReturnLinkMessage, sso = true)
      )
      accountSummary.status shouldBe success

    }

    "return the default summary if the account summary link is missing" in {
      val regimeRootsWithNoCtAccountSummary = RegimeRoots(ct = Some(CtRoot(ctUtr, Map[String, String]())))
      val userEnrolledForCtWithNoAccountSummary = User("tim", ctAuthority("tim", "12347"), regimeRootsWithNoCtAccountSummary, None, None)
      val mockCtConnector = mock[CtConnector]
      val builder = new CtAccountSummaryBuilder(mockCtConnector)

      val accountSummaryOption: Option[Future[AccountSummary]] = builder.build(buildPortalUrl, userEnrolledForCtWithNoAccountSummary)
      accountSummaryOption should not be None
      val accountSummary = await(accountSummaryOption.get)
      accountSummary.messages shouldBe Seq[Msg](Msg(ctUtrMessage, Seq(ctUtr.utr)), Msg(ctSummaryUnavailableErrorMessage1),
        Msg(ctSummaryUnavailableErrorMessage2), Msg(ctSummaryUnavailableErrorMessage3), Msg(ctSummaryUnavailableErrorMessage4))
      accountSummary.addenda shouldBe Seq.empty
      accountSummary.status shouldBe default
    }

//    "return the oops summary if there is an exception when requesting the root" in {
//      val regimeRoots = RegimeRoots(ct = Some((new NumberFormatException)))
//      val user = User("tim", userAuthorityWithCt, regimeRoots, None, None)
//      val mockCtConnector = mock[CtConnector]
//      val builder = new CtAccountSummaryBuilder(mockCtConnector)
//      val accountSummaryOption: Option[AccountSummary] = builder.build(buildPortalUrl, user)
//      accountSummaryOption should not be None
//      val accountSummary = accountSummaryOption.get
//      accountSummary.regimeName shouldBe ctRegimeNameMessage
//      accountSummary.messages shouldBe Seq[Msg](Msg(oopsMessage, Seq.empty))
//      accountSummary.addenda shouldBe Seq.empty
//      accountSummary.status shouldBe oops
//      verifyZeroInteractions(mockCtConnector)
//    }

    "return the oops summary if there is an exception when requesting the account summary" in {
      val mockCtConnector = mock[CtConnector]
      when(mockCtConnector.accountSummary(s"/ct/${ctUtr.utr}/account-summary")).thenThrow(new NumberFormatException)
      val builder = new CtAccountSummaryBuilder(mockCtConnector)

      val accountSummaryOption: Option[Future[AccountSummary]] = builder.build(buildPortalUrl, userEnrolledForCt)
      accountSummaryOption should not be None
      val accountSummary = accountSummaryOption.get
      accountSummary.regimeName shouldBe ctRegimeNameMessage
      accountSummary.messages shouldBe Seq[Msg](Msg(oopsMessage, Seq.empty))
      accountSummary.addenda shouldBe Seq.empty
      accountSummary.status shouldBe oops
    }

    "return None if the user is not enrolled for CT" in {
      val builder = new CtAccountSummaryBuilder(mock[CtConnector])
      val accountSummaryOption: Option[Future[AccountSummary]] = builder.build(buildPortalUrl, userNotEnrolledForCt)
      accountSummaryOption should be(None)
    }
  }
}
