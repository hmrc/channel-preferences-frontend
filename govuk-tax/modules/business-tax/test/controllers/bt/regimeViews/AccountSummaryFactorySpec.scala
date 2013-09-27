package controllers.bt.regimeViews

import ct.CtMicroService
import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import controllers.bt.{AccountSummary, AccountSummariesFactory}
import uk.gov.hmrc.common.microservice.vat.VatMicroService
import uk.gov.hmrc.common.microservice.sa.SaMicroService
import uk.gov.hmrc.common.microservice.epaye.EPayeConnector
import scala.Predef._
import uk.gov.hmrc.common.microservice.domain.User
import org.mockito.Mockito._


class AccountSummaryFactorySpec extends BaseSpec with MockitoSugar {

  def mockSaRegimeAccountSummaryViewBuilder = mock[SaAccountSummaryViewBuilder]
  def mockVatRegimeAccountSummaryViewBuilder = mock[VatAccountSummaryViewBuilder]
  def mockCtRegimeAccountSummaryViewBuilder = mock[CtAccountSummaryViewBuilder]
  def mockEPayeRegimeAccountSummaryViewBuilder = mock[EPayeAccountSummaryViewBuilder]
  def mockUser = mock[User]

      def factory() = new AccountSummariesFactory(mock[SaMicroService], mock[VatMicroService],mock[CtMicroService], mock[EPayeConnector]) {
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
