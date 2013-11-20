package controllers.bt.accountsummary

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import scala.Predef._
import uk.gov.hmrc.common.microservice.domain.User
import org.mockito.Mockito._
import controllers.common.actions.HeaderCarrier


class AccountSummariesFactorySpec extends BaseSpec with MockitoSugar {

  val mockSaRegimeAccountSummaryViewBuilder = mock[SaAccountSummaryBuilder]

  val mockVatRegimeAccountSummaryViewBuilder = mock[VatAccountSummaryBuilder]

  val mockCtRegimeAccountSummaryViewBuilder = mock[CtAccountSummaryBuilder]

  val mockEpayeRegimeAccountSummaryViewBuilder = mock[EpayeAccountSummaryBuilder]

  val mockUser = mock[User]

  before {
    reset(mockSaRegimeAccountSummaryViewBuilder,
      mockVatRegimeAccountSummaryViewBuilder,
      mockCtRegimeAccountSummaryViewBuilder,
      mockEpayeRegimeAccountSummaryViewBuilder,
      mockUser)
  }

  def factory() = new AccountSummariesFactory(
    mockSaRegimeAccountSummaryViewBuilder,
    mockVatRegimeAccountSummaryViewBuilder,
    mockCtRegimeAccountSummaryViewBuilder,
    mockEpayeRegimeAccountSummaryViewBuilder)

  "Account Summary Factory when constructor " should {
    "construct an AccountSummaries model using the AccountSummary regimes factories that return None" in {
      val factoryUnderTest = factory()
      val userUnderTest = mockUser
      val portalBuilder: (String => String) = (value) => value

      implicit val headerCarrier = HeaderCarrier()
      when(mockSaRegimeAccountSummaryViewBuilder.build(portalBuilder, userUnderTest)).thenReturn(None)
      when(mockVatRegimeAccountSummaryViewBuilder.build(portalBuilder, userUnderTest)).thenReturn(None)
      when(mockCtRegimeAccountSummaryViewBuilder.build(portalBuilder, userUnderTest)).thenReturn(None)
      when(mockEpayeRegimeAccountSummaryViewBuilder.build(portalBuilder, userUnderTest)).thenReturn(None)

      val actualAccountSummaries = factoryUnderTest.create(portalBuilder)(userUnderTest, headerCarrier)
      actualAccountSummaries.regimes shouldBe Seq.empty
    }

    "construct an AccountSummaries model using the AccountSummary regimes factories that return models" in {
      val saAccountSummary = AccountSummary("SA", Seq.empty, Seq.empty, SummaryStatus.success)
      val vatAccountSummary = AccountSummary("VAT", Seq.empty, Seq.empty, SummaryStatus.success)
      val ctAccountSummary = AccountSummary("CT", Seq.empty, Seq.empty, SummaryStatus.success)
      val epayeAccountSummary = AccountSummary("EPAYE", Seq.empty, Seq.empty, SummaryStatus.success)

      val factoryUnderTest = factory()
      val userUnderTest = mockUser
      val portalBuilder: (String => String) = (value) => value

      implicit val headerCarrier = HeaderCarrier()
      when(mockSaRegimeAccountSummaryViewBuilder.build(portalBuilder, userUnderTest)).thenReturn(Some(saAccountSummary))
      when(mockVatRegimeAccountSummaryViewBuilder.build(portalBuilder, userUnderTest)).thenReturn(Some(vatAccountSummary))
      when(mockCtRegimeAccountSummaryViewBuilder.build(portalBuilder, userUnderTest)).thenReturn(Some(ctAccountSummary))
      when(mockEpayeRegimeAccountSummaryViewBuilder.build(portalBuilder, userUnderTest)).thenReturn(Some(epayeAccountSummary))

      val actualAccountSummaries = factoryUnderTest.create(portalBuilder)(userUnderTest, headerCarrier)
      actualAccountSummaries.regimes shouldBe List(saAccountSummary, ctAccountSummary, vatAccountSummary, epayeAccountSummary)
    }

    "keeps the AccountSummaries in the right order" in {

      val saAccountSummary = AccountSummary("SA", Seq.empty, Seq.empty, SummaryStatus.success)
      val vatAccountSummary = AccountSummary("VAT", Seq.empty, Seq.empty, SummaryStatus.success)
      val ctAccountSummary = AccountSummary("CT", Seq.empty, Seq.empty, SummaryStatus.success)
      val epayeAccountSummary = AccountSummary("EPAYE", Seq.empty, Seq.empty, SummaryStatus.success)

      val factoryUnderTest = factory()
      val userUnderTest = mockUser
      val portalBuilder: (String => String) = (value) => value

      implicit val headerCarrier = HeaderCarrier()
      when(mockSaRegimeAccountSummaryViewBuilder.build(portalBuilder, userUnderTest)).thenReturn(Some(saAccountSummary))
      when(mockVatRegimeAccountSummaryViewBuilder.build(portalBuilder, userUnderTest)).thenReturn(Some(vatAccountSummary))
      when(mockCtRegimeAccountSummaryViewBuilder.build(portalBuilder, userUnderTest)).thenReturn(Some(ctAccountSummary))
      when(mockEpayeRegimeAccountSummaryViewBuilder.build(portalBuilder, userUnderTest)).thenReturn(Some(epayeAccountSummary))

      val actualAccountSummaries = factoryUnderTest.create(portalBuilder)(userUnderTest, headerCarrier)
      val expectedListInOrder = List(saAccountSummary, ctAccountSummary, vatAccountSummary, epayeAccountSummary)
      actualAccountSummaries.regimes shouldBe expectedListInOrder

    }

  }
}
