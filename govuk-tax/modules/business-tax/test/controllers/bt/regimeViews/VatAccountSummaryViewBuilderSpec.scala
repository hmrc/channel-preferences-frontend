package controllers.bt.regimeViews

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.microservice.domain.{RegimeRoots, User}
import uk.gov.hmrc.common.microservice.vat.VatMicroService
import uk.gov.hmrc.microservice.auth.domain.{Vrn, Regimes, UserAuthority}
import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.{VatAccountBalance, VatAccountSummary, VatRoot}
import org.mockito.Mockito._
import views.helpers.{MoneyPounds, RenderableMoneyMessage, RenderableMessage, LinkMessage}

class VatAccountSummaryViewBuilderSpec extends BaseSpec with MockitoSugar {

  val buildPortalUrl: (String) => String = (value: String) => value
  val vrn = Vrn("12345")
  val userAuthorityWithVrn = UserAuthority("123", Regimes(), vrn = Some(vrn))
  val aDate = "2012-06-06"
  val regimeRootsWithVat = RegimeRoots(None, None, Some(VatRoot(vrn, Map("accountSummary" -> s"/vat/vrn/${vrn.vrn}"))))
  val userEnrolledForVat = User("jim", userAuthorityWithVrn, regimeRootsWithVat, None, None)

  "VatAccountSummaryViewBuilder" should {
    "return the correct account summary for complete data" in {

      val accountSummary = VatAccountSummary(Some(VatAccountBalance(Some(6.1), Some("GBP"))), Some(aDate))
      val mockVatMicroService = mock[VatMicroService]
      when(mockVatMicroService.accountSummary(s"/vat/vrn/${vrn.vrn}")).thenReturn(Some(accountSummary))
      val builder: VatAccountSummaryViewBuilder = VatAccountSummaryViewBuilder(buildPortalUrl, userEnrolledForVat, mockVatMicroService)
      val accountSummaryViewOption = builder.build
      accountSummaryViewOption shouldNot be(None)
      val accountSummaryView = accountSummaryViewOption.get
      accountSummaryView.regimeName shouldBe "VAT"
      accountSummaryView.messages shouldBe Seq[(String, Seq[RenderableMessage])]("vat.message.0" -> Seq("12345"), "vat.message.1" -> Seq(MoneyPounds(BigDecimal(6.1))))
      accountSummaryView.addenda shouldBe Seq[RenderableMessage](LinkMessage("vatAccountDetails", "vat.accountSummary.linkText.accountDetails"),
        LinkMessage("/makeAPaymentLanding", "vat.accountSummary.linkText.makeAPayment"),
        LinkMessage("vatFileAReturn", "vat.accountSummary.linkText.fileAReturn"))
    }

    "return an error message if the account summary is not available" in {

      val accountSummary = VatAccountSummary(Some(VatAccountBalance(None, Some("GBP"))), Some(aDate))
      val mockVatMicroService = mock[VatMicroService]
      when(mockVatMicroService.accountSummary(s"/vat/vrn/${vrn.vrn}")).thenReturn(Some(accountSummary))
      val builder: VatAccountSummaryViewBuilder = VatAccountSummaryViewBuilder(buildPortalUrl, userEnrolledForVat, mockVatMicroService)
      val accountSummaryViewOption = builder.build
      accountSummaryViewOption shouldNot be(None)
      val accountSummaryView = accountSummaryViewOption.get
      accountSummaryView.regimeName shouldBe "VAT"
      accountSummaryView.messages shouldBe Seq[(String, Seq[RenderableMessage])](("vat.error.message.summaryUnavailable.1", Seq.empty), ("vat.error.message.summaryUnavailable.2", Seq.empty),
        ("vat.error.message.summaryUnavailable.3", Seq.empty), ("vat.error.message.summaryUnavailable.4", Seq(LinkMessage("/TODO/HelpDeskLink", "vat.accountSummary.linkText.helpDesk"))))
      accountSummaryView.addenda shouldBe Seq.empty
    }

    "return None if the user is not enrolled for VAT" in {

      val userAuthority = UserAuthority("123", Regimes())
      val regimeRoots = RegimeRoots(None, None, None)
      val user = User("jim", userAuthority, regimeRoots, None, None)
      val mockVatMicroService = mock[VatMicroService]
      val builder: VatAccountSummaryViewBuilder = VatAccountSummaryViewBuilder(buildPortalUrl, user, mockVatMicroService)
      val accountSummaryViewOption = builder.build
      accountSummaryViewOption shouldBe None
    }
  }
}
