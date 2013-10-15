package controllers.bt.regimeViews

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.vat.VatConnector
import uk.gov.hmrc.common.microservice.sa.SaConnector
import uk.gov.hmrc.common.microservice.epaye.EpayeConnector
import scala.Predef._
import uk.gov.hmrc.common.microservice.domain.User
import org.mockito.Mockito._
import uk.gov.hmrc.common.microservice.ct.CtConnector


class AccountSummariesFactorySpec extends BaseSpec with MockitoSugar {

  def mockSaRegimeAccountSummaryViewBuilder = mock[SaAccountSummaryBuilder]
  def mockVatRegimeAccountSummaryViewBuilder = mock[VatAccountSummaryBuilder]
  def mockCtRegimeAccountSummaryViewBuilder = mock[CtAccountSummaryBuilder]
  def mockEpayeRegimeAccountSummaryViewBuilder = mock[EpayeAccountSummaryBuilder]

  def mockUser = mock[User]

  def factory() = new AccountSummariesFactory(null, null, null, null) {
    override val saRegimeAccountSummaryViewBuilder = mockSaRegimeAccountSummaryViewBuilder
    override val vatRegimeAccountSummaryViewBuilder = mockVatRegimeAccountSummaryViewBuilder
    override val ctRegimeAccountSummaryViewBuilder = mockCtRegimeAccountSummaryViewBuilder
    override val epayeRegimeAccountSummaryViewBuilder = mockEpayeRegimeAccountSummaryViewBuilder
  }

  "Account Summary Factory when constructor " should {
    "construct an AccountSummaries model using the AccountSummary regimes factories that return None" in {
      val factoryUnderTest = factory()
      val userUnderTest = mockUser
      val portalBuilder: (String => String) = (value) => value

      when(factoryUnderTest.saRegimeAccountSummaryViewBuilder.build(portalBuilder, userUnderTest)).thenReturn(None)
      when(factoryUnderTest.vatRegimeAccountSummaryViewBuilder.build(portalBuilder, userUnderTest)).thenReturn(None)
      when(factoryUnderTest.ctRegimeAccountSummaryViewBuilder.build(portalBuilder, userUnderTest)).thenReturn(None)
      when(factoryUnderTest.epayeRegimeAccountSummaryViewBuilder.build(portalBuilder, userUnderTest)).thenReturn(None)

      val actualAccountSummaries = factoryUnderTest.create(portalBuilder)(userUnderTest)
      actualAccountSummaries.regimes shouldBe Seq.empty
    }

    "construct an AccountSummaries model using the AccountSummary regimes factories that return models" in {
      val saAccountSummary = AccountSummary("SA", Seq.empty, Seq.empty, SummaryStatus.success)
      val vatAccountSummary = AccountSummary("SA", Seq.empty, Seq.empty, SummaryStatus.success)
      val ctAccountSummary = AccountSummary("SA", Seq.empty, Seq.empty, SummaryStatus.success)
      val epayeAccountSummary = AccountSummary("SA", Seq.empty, Seq.empty, SummaryStatus.success)

      val factoryUnderTest = factory()
      val userUnderTest = mockUser
      val portalBuilder: (String => String) = (value) => value
      when(factoryUnderTest.saRegimeAccountSummaryViewBuilder.build(portalBuilder, userUnderTest)).thenReturn(Some(saAccountSummary))
      when(factoryUnderTest.vatRegimeAccountSummaryViewBuilder.build(portalBuilder, userUnderTest)).thenReturn(Some(vatAccountSummary))
      when(factoryUnderTest.ctRegimeAccountSummaryViewBuilder.build(portalBuilder, userUnderTest)).thenReturn(Some(ctAccountSummary))
      when(factoryUnderTest.epayeRegimeAccountSummaryViewBuilder.build(portalBuilder, userUnderTest)).thenReturn(Some(epayeAccountSummary))

      val actualAccountSummaries = factoryUnderTest.create(portalBuilder)(userUnderTest)
      actualAccountSummaries.regimes shouldBe Seq(saAccountSummary, vatAccountSummary, ctAccountSummary, epayeAccountSummary)
    }
  }
}
