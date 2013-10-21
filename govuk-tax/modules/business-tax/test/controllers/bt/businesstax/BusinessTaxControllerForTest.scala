package controllers.bt.businesstax

import play.api.test.{FakeApplication, WithApplication}
import controllers.common.CookieEncryption
import controllers.bt.testframework.mocks.{DateTimeProviderMock, ConnectorMocks, PortalUrlBuilderMock}
import controllers.bt.accountsummary.{AccountSummaries, AccountSummariesFactory}
import org.scalatest.mock.MockitoSugar
import play.api.templates.Html
import uk.gov.hmrc.common.microservice.domain.User
import controllers.bt.BusinessTaxController


abstract class BusinessTaxControllerForTest extends WithApplication(FakeApplication()) with CookieEncryption with PortalUrlBuilderMock with ConnectorMocks with DateTimeProviderMock with BusinessTaxPageMocks {

  val mockAccountSummariesFactory = mock[AccountSummariesFactory]
  val controllerUnderTest = new BusinessTaxController(mockAccountSummariesFactory) with MockedPortalUrlBuilder with MockedConnectors with MockedDateTimeProvider with MockedBusinessTaxPages

}

trait BusinessTaxPageMocks extends MockitoSugar {

  val mockBusinessTaxPages = mock[MockableBusinessTaxPages]

  trait MockableBusinessTaxPages {
    def makeAPaymentLandingPage(): Html
    def businessTaxHomepage(portalHref: String, accountSummaries: AccountSummaries): Html
  }

  trait MockedBusinessTaxPages {
    self: BusinessTaxController =>

    override private[bt] def makeAPaymentLandingPage()(implicit user: User) =
      mockBusinessTaxPages.makeAPaymentLandingPage()

    override private[bt] def businessTaxHomepage(portalHref: String, accountSummaries: AccountSummaries)(implicit user: User) =
      mockBusinessTaxPages.businessTaxHomepage(portalHref, accountSummaries)
  }

}

