package controllers.bt.regimeViews

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, User}
import uk.gov.hmrc.common.microservice.vat.VatMicroService
import uk.gov.hmrc.common.microservice.auth.domain.{Regimes, UserAuthority}
import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.{VatAccountBalance, VatAccountSummary, VatRoot}
import org.mockito.Mockito._
import views.helpers.{MoneyPounds, RenderableMessage, LinkMessage}
import uk.gov.hmrc.domain.Vrn
import VatMessageKeys._
import VatPortalUrls._

class VatAccountSummaryBuilderSpec extends BaseSpec with MockitoSugar {

  val buildPortalUrl: (String) => String = (value: String) => value
  val vrn = Vrn("12345")
  val userAuthorityWithVrn = UserAuthority("123", Regimes(), vrn = Some(vrn))
  val aDate = "2012-06-06"
  val regimeRootsWithVat = RegimeRoots(None, None, Some(VatRoot(vrn, Map("accountSummary" -> s"/vat/${vrn.vrn}"))), None, None)
  val userEnrolledForVat = User("jim", userAuthorityWithVrn, regimeRootsWithVat, None, None)

  "VatAccountSummaryViewBuilder" should {
    "return the correct account summary for complete data" in {

      val accountSummary = VatAccountSummary(Some(VatAccountBalance(Some(6.1), Some("GBP"))), Some(aDate))
      val mockVatMicroService = mock[VatMicroService]
      when(mockVatMicroService.accountSummary(s"/vat/${vrn.vrn}")).thenReturn(Some(accountSummary))
      val builder: VatAccountSummaryBuilder = VatAccountSummaryBuilder(mockVatMicroService)
      val accountSummaryViewOption = builder.build(buildPortalUrl, userEnrolledForVat)
      accountSummaryViewOption shouldNot be(None)
      val accountSummaryView = accountSummaryViewOption.get
      accountSummaryView.regimeName shouldBe vatRegimeNameMessage
      accountSummaryView.messages shouldBe Seq[(String, Seq[RenderableMessage])](vatRegistrationNumberMessage -> Seq("12345"), vatAccountBalanceMessage -> Seq(MoneyPounds(BigDecimal(6.1))))
      accountSummaryView.addenda shouldBe Seq[RenderableMessage](LinkMessage(vatAccountDetailsPortalUrl, viewAccountDetailsLinkMessage),
        LinkMessage("/makeAPaymentLanding", makeAPaymentLinkMessage), // TODO [JJS] WHAT'S THIS LINK?
        LinkMessage(vatFileAReturnPortalUrl, fileAReturnLinkMessage))
    }

    "return an error message if the account summary is not available" in {

      val accountSummary = VatAccountSummary(Some(VatAccountBalance(None, Some("GBP"))), Some(aDate))
      val mockVatMicroService = mock[VatMicroService]
      when(mockVatMicroService.accountSummary(s"/vat/${vrn.vrn}")).thenReturn(Some(accountSummary))
      val builder: VatAccountSummaryBuilder = VatAccountSummaryBuilder(mockVatMicroService)
      val accountSummaryViewOption = builder.build(buildPortalUrl, userEnrolledForVat)
      accountSummaryViewOption shouldNot be(None)
      val accountSummaryView = accountSummaryViewOption.get
      accountSummaryView.regimeName shouldBe vatRegimeNameMessage
      accountSummaryView.messages shouldBe Seq[(String, Seq[RenderableMessage])]((vatSummaryUnavailableErrorMessage1, Seq.empty), (vatSummaryUnavailableErrorMessage2, Seq.empty),
        (vatSummaryUnavailableErrorMessage3, Seq.empty), (vatSummaryUnavailableErrorMessage4, Seq(LinkMessage(vatHelpDeskPortalUrl, vatHelpDeskLinkMessage))))
      accountSummaryView.addenda shouldBe Seq.empty
    }

    "return None if the user is not enrolled for VAT" in {

      val userAuthority = UserAuthority("123", Regimes())
      val regimeRoots = RegimeRoots(None, None, None, None, None)
      val user = User("jim", userAuthority, regimeRoots, None, None)
      val mockVatMicroService = mock[VatMicroService]
      val builder: VatAccountSummaryBuilder = VatAccountSummaryBuilder(mockVatMicroService)
      val accountSummaryViewOption = builder.build(buildPortalUrl, user)
      accountSummaryViewOption shouldBe None
    }
  }
}
