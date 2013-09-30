package controllers.bt.regimeViews

import uk.gov.hmrc.domain.{Vrn, CtUtr}
import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import uk.gov.hmrc.common.microservice.auth.domain.{Regimes, UserAuthority}
import java.net.URI
import views.helpers.{LinkMessage, MoneyPounds, RenderableMessage}
import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.VatRoot
import org.joda.time.LocalDate
import org.joda.time.chrono.ISOChronology
import CtMessageKeys._
import CtPortalUrlKeys._
import uk.gov.hmrc.common.microservice.ct.domain.CtDomain.{CtAccountBalance, CtAccountSummary, CtRoot}
import uk.gov.hmrc.common.microservice.ct.CtConnector

class CtAccountSummaryBuilderSpec extends BaseSpec with MockitoSugar {
  val buildPortalUrl: (String) => String = (value: String) => value
  val ctUtr = CtUtr("12347")
  val aDate = "2012-06-06"
  val regimeRootsWithCt = RegimeRoots(None, None, None, None, Some(CtRoot(Map("accountSummary" -> s"/ct/${ctUtr.utr}/account-summary"))))
  val userAuthorityWithCt = UserAuthority("123", Regimes(ct = Some(new URI(s"/ct/${ctUtr.utr}"))), ctUtr = Some(ctUtr))
  val userEnrolledForCt = User("tim", userAuthorityWithCt, regimeRootsWithCt, None, None)
  val vrn = Vrn("123")
  val userAuthorityWithoutCt = UserAuthority("123", Regimes(), vrn = Some(vrn))
  val regimeRootsWithoutCt = RegimeRoots(None, None, Some(VatRoot(vrn, Map("accountSummary" -> s"/vat/vrn/${vrn.vrn}"))), None, None)
  val userNotEnrolledForCt = User("jim", userAuthorityWithoutCt, regimeRootsWithoutCt, None, None)

  "CtAccountSummaryViewBuilder" should {

    "return the correct account summary for complete data" in {
      val ctMicroSeriveMock = mock[CtConnector]
      val ctAccountSummary = CtAccountSummary(Some(CtAccountBalance(Some(4.2), Some("GPB"))), Some("2012-12-02"))
      when(ctMicroSeriveMock.accountSummary(s"/ct/${ctUtr.utr}/account-summary")).thenReturn(Some(ctAccountSummary))
      val builder = new CtAccountSummaryBuilder(ctMicroSeriveMock)
      val accountSummaryOption: Option[AccountSummary] = builder.build(buildPortalUrl, userEnrolledForCt)
      accountSummaryOption should not be None
      val accountSummary = accountSummaryOption.get
      accountSummary.regimeName shouldBe ctRegimeNameMessage
      accountSummary.messages shouldBe Seq[(String, Seq[RenderableMessage])](ctUtrMessage -> Seq("12347"), ctAmountAsOfDateMessage -> Seq(MoneyPounds(BigDecimal(4.2)), new LocalDate(2012, 12, 2, ISOChronology.getInstanceUTC)))
      accountSummary.addenda shouldBe Seq[RenderableMessage](LinkMessage(ctAccountDetailsPortalUrl, viewAccountDetailsLinkMessage),
        LinkMessage("/makeAPaymentLanding", makeAPaymentLinkMessage),
        LinkMessage(ctFileAReturnPortalUrl, fileAReturnLinkMessage))

    }

    "return an error message if the account summary is not available" in {
      val ctMicroSeriveMock = mock[CtConnector]
      when(ctMicroSeriveMock.accountSummary(s"/ct/${ctUtr.utr}/account-summary")).thenReturn(None)
      val builder = new CtAccountSummaryBuilder(ctMicroSeriveMock)
      val accountSummaryOption: Option[AccountSummary] = builder.build(buildPortalUrl, userEnrolledForCt)
      accountSummaryOption should not be None
      val accountSummary = accountSummaryOption.get
      accountSummary.messages shouldBe Seq[(String, Seq[RenderableMessage])](ctUtrMessage -> Seq(ctUtr.utr), (ctSummaryUnavailableErrorMessage1, Seq.empty), (ctSummaryUnavailableErrorMessage2, Seq.empty),
        (ctSummaryUnavailableErrorMessage3, Seq.empty), (ctSummaryUnavailableErrorMessage4, Seq.empty))
      accountSummary.addenda shouldBe Seq.empty


    }

    "return None if the user is not enrolled for VAT" in {
      val builder = new CtAccountSummaryBuilder(mock[CtConnector])
      val accountSummaryOption: Option[AccountSummary] = builder.build(buildPortalUrl, userNotEnrolledForCt)
      accountSummaryOption should be(None)

    }
  }
}
