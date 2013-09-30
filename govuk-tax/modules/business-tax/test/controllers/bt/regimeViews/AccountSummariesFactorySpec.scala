package controllers.bt.regimeViews

import ct.CtConnector
import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.vat.VatConnector
import uk.gov.hmrc.common.microservice.sa.SaConnector
import uk.gov.hmrc.common.microservice.epaye.EPayeConnector
import scala.Predef._
import uk.gov.hmrc.common.microservice.domain.User
import org.mockito.Mockito._


class AccountSummariesFactorySpec extends BaseSpec with MockitoSugar {

  def mockSaRegimeAccountSummaryViewBuilder = mock[SaAccountSummaryBuilder]
  def mockVatRegimeAccountSummaryViewBuilder = mock[VatAccountSummaryBuilder]
  def mockCtRegimeAccountSummaryViewBuilder = mock[CtAccountSummaryBuilder]
  def mockEPayeRegimeAccountSummaryViewBuilder = mock[EPayeAccountSummaryBuilder]
  def mockUser = mock[User]

      def factory() = new AccountSummariesFactory(mock[SaConnector], mock[VatConnector],mock[CtConnector], mock[EPayeConnector]) {
        override val saRegimeAccountSummaryViewBuilder = mockSaRegimeAccountSummaryViewBuilder
        override val vatRegimeAccountSummaryViewBuilder = mockVatRegimeAccountSummaryViewBuilder
        override val ctRegimeAccountSummaryViewBuilder = mockCtRegimeAccountSummaryViewBuilder
        override val epayeRegimeAccountSummaryViewBuilder = mockEPayeRegimeAccountSummaryViewBuilder
      }

  "Account Summary Factory when constructor " should {
    "construct an AccountSummaries model using the AccountSummary regimes factories that return None" in {
      val factoryUnderTest = factory()
      val userUnderTest = mockUser
      val portalBuilder : (String => String) = (value) => value
      when(factoryUnderTest.saRegimeAccountSummaryViewBuilder.build(portalBuilder, userUnderTest)).thenReturn(None)
      when(factoryUnderTest.vatRegimeAccountSummaryViewBuilder.build(portalBuilder, userUnderTest)).thenReturn(None)
      when(factoryUnderTest.ctRegimeAccountSummaryViewBuilder.build(portalBuilder, userUnderTest)).thenReturn(None)
      when(factoryUnderTest.epayeRegimeAccountSummaryViewBuilder.build(portalBuilder, userUnderTest)).thenReturn(None)


      val actualAccountSummaries = factoryUnderTest.create(portalBuilder)(userUnderTest)
      actualAccountSummaries.regimes shouldBe Seq.empty
    }

    "construct an AccountSummaries model using the AccountSummary regimes factories that return models" in {
      val saAccountSummary = AccountSummary("SA", Seq.empty, Seq.empty)
      val vatAccountSummary = AccountSummary("SA", Seq.empty, Seq.empty)
      val ctAccountSummary = AccountSummary("SA", Seq.empty, Seq.empty)
      val epayeAccountSummary = AccountSummary("SA", Seq.empty, Seq.empty)

      val factoryUnderTest = factory()
      val userUnderTest = mockUser
      val portalBuilder : (String => String) = (value) => value
      when(factoryUnderTest.saRegimeAccountSummaryViewBuilder.build(portalBuilder, userUnderTest)).thenReturn(Some(saAccountSummary))
      when(factoryUnderTest.vatRegimeAccountSummaryViewBuilder.build(portalBuilder, userUnderTest)).thenReturn(Some(vatAccountSummary))
      when(factoryUnderTest.ctRegimeAccountSummaryViewBuilder.build(portalBuilder, userUnderTest)).thenReturn(Some(ctAccountSummary))
      when(factoryUnderTest.epayeRegimeAccountSummaryViewBuilder.build(portalBuilder, userUnderTest)).thenReturn(Some(epayeAccountSummary))


      val actualAccountSummaries = factoryUnderTest.create(portalBuilder)(userUnderTest)
      actualAccountSummaries.regimes shouldBe Seq(saAccountSummary, vatAccountSummary, ctAccountSummary, epayeAccountSummary)
    }
  }
}
