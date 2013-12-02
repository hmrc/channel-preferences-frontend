package uk.gov.hmrc.common

import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import play.api.test.{ FakeApplication, WithApplication }
import play.api.mvc.{ Session, Request }

class PortalUrlBuilderSpec extends BaseSpec with MockitoSugar {

  import controllers.domain.AuthorityUtils._

  val portalUrlBuilder = new PortalUrlBuilder {}
  
  val testConfig = Map(
    "govuk-tax.Test.portal.destinationPath.someDestinationPathKey" -> "http://someserver:8080/utr/<utr>/year/<year>",
    "govuk-tax.Test.portal.destinationPath.anotherDestinationPathKey" -> "http://someserver:8080/utr/<utr>/year/<year>",
    "govuk-tax.Test.portal.destinationPath.testVatAccountDetails" -> "http://someserver:8080/vat/trader/<vrn>/account",
    "govuk-tax.Test.portal.destinationPath.testCtAccountDetails" -> "http://someserver:8080/corporation-tax/org/<ctutr>/account",
    "govuk-tax.Test.portal.destinationRoot" -> "http://someOtherserver:8080",
    "cookie.encryption.key" -> "gvBoGdgzqG1AarzF1LY0zQ=="
  )
  
  "PortalUrlBuilder " should {

    "return a resolved dynamic full URL with parameters year and saUtr resolved using a request and user object" in new WithApplication(FakeApplication(additionalConfiguration = testConfig)) {
      implicit val mockRequest= mock[Request[AnyRef]]
      implicit val mockUser = mock[User]
      val saUtr = "someUtr"

       when(mockUser.userAuthority).thenReturn(saAuthority("123", saUtr))

      val actualDestinationUrl = portalUrlBuilder.buildPortalUrl("anotherDestinationPathKey")

      actualDestinationUrl should startWith("""http://someserver:8080/utr/someUtr/year""")
      actualDestinationUrl should not endWith """<year>"""
    }

    "return a URL without the UTR resolved when the utr is missing" in new WithApplication(FakeApplication(additionalConfiguration = testConfig)) {
      implicit val mockRequest= mock[Request[AnyRef]]
      implicit val mockUser = mock[User]
      val mockSession = mock[Session]

      when(mockUser.userAuthority).thenReturn(payeAuthority("", "someNino"))

      val actualDestinationUrl = portalUrlBuilder.buildPortalUrl("anotherDestinationPathKey")

      actualDestinationUrl should startWith("""http://someserver:8080/utr/<utr>/year""")
      actualDestinationUrl should not endWith """<year>"""
    }

    "return a URL which is resolved using a vrn parameter" in new WithApplication(FakeApplication(additionalConfiguration = testConfig)) {
      implicit val mockRequest= mock[Request[AnyRef]]
      implicit val mockUser = mock[User]
      when(mockUser.userAuthority).thenReturn(vatAuthority("", "someVrn"))

      val actualDestinationUrl = portalUrlBuilder.buildPortalUrl("testVatAccountDetails")

      actualDestinationUrl should startWith("""http://someserver:8080/vat/trader/someVrn/account""")
    }

    "return an invalid URL when we request a link requiring a vrn parameter but the vrn is missing" in new WithApplication(FakeApplication(additionalConfiguration = testConfig)) {
      implicit val mockRequest= mock[Request[AnyRef]]
      implicit val mockUser = mock[User]

      when(mockUser.userAuthority).thenReturn(saAuthority("", ""))

      val actualDestinationUrl = portalUrlBuilder.buildPortalUrl("testVatAccountDetails")

      actualDestinationUrl should startWith("""http://someserver:8080/vat/trader/<vrn>/account""")
    }

    "return a URL which is resolved using a ctUtr parameter" in new WithApplication(FakeApplication(additionalConfiguration = testConfig)) {
      implicit val mockRequest= mock[Request[AnyRef]]
      implicit val mockUser = mock[User]
      when(mockUser.userAuthority).thenReturn(ctAuthority("", "someCtUtr"))

      val actualDestinationUrl = portalUrlBuilder.buildPortalUrl("testCtAccountDetails")

      actualDestinationUrl should startWith("""http://someserver:8080/corporation-tax/org/someCtUtr/account""")
    }

    "return an invalid URL when we request a link requiring a ctUtr parameter but the ctUtr is missing" in new WithApplication(FakeApplication(additionalConfiguration = testConfig)) {
      implicit val mockRequest= mock[Request[AnyRef]]
      implicit val mockUser = mock[User]
      val ctUtr = "someCtUtr"

      when(mockUser.userAuthority).thenReturn(vatAuthority("", "vrn"))

      val actualDestinationUrl = portalUrlBuilder.buildPortalUrl("testCtAccountDetails")

      actualDestinationUrl should startWith("""http://someserver:8080/corporation-tax/org/<ctutr>/account""")
    }
  }
}
