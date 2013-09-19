package uk.gov.hmrc.common

import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import play.api.test.{ FakeApplication, WithApplication }
import play.api.mvc.{ Session, Request }
import controllers.common.CookieEncryption
import uk.gov.hmrc.domain.{Vrn, SaUtr}

class PortalDestinationUrlBuilderSpec extends BaseSpec with MockitoSugar with CookieEncryption {

  val mockConfigValues = Map("govuk-tax.Test.portal.destinationPath.someDestinationPathKey" -> "/utr/<utr>/year/<year>",
    "govuk-tax.Test.portal.destinationPath.anotherDestinationPathKey" -> "/utr/<utr>/affinitygroup/<affinitygroup>/year/<year>",
    "govuk-tax.Test.portal.destinationPath.testVatAccountDetails" -> "/vat/trader/<vrn>/account",
    "govuk-tax.Test.portal.destinationRoot" -> "http://someserver:8080",
    "cookie.encryption.key" -> "gvBoGdgzqG1AarzF1LY0zQ=="
  )

  "PortalDestinationUrlBuilder " should {

    "return a resolved dynamic full URL with parameters year, saUtr and affinity group resolved using a request and user object" in new WithApplication(FakeApplication(additionalConfiguration = mockConfigValues)) {
      val mockRequest = mock[Request[AnyRef]]
      val mockSession = mock[Session]
      val mockUser = mock[User]
      val mockUserAuthority = mock[UserAuthority]
      val saUtr = "someUtr"

      when(mockRequest.session).thenReturn(mockSession)
      when(mockSession.get("affinityGroup")).thenReturn(Some(encrypt("someaffinitygroup")))
      when(mockUser.userAuthority).thenReturn(mockUserAuthority)
      when(mockUserAuthority.saUtr).thenReturn(Some(SaUtr(saUtr)))

      val portalUrlBuilder = PortalDestinationUrlBuilder.build(mockRequest, mockUser) _
      val actualDestinationUrl = portalUrlBuilder("anotherDestinationPathKey")

      actualDestinationUrl should startWith("""http://someserver:8080/utr/someUtr/affinitygroup/someaffinitygroup/year""")
      actualDestinationUrl should not endWith ("""<year>""")
    }

    "throw an exception when the affinity group is missing" in new WithApplication(FakeApplication(additionalConfiguration = mockConfigValues)) {
      val mockRequest = mock[Request[AnyRef]]
      val mockSession = mock[Session]
      val mockUser = mock[User]
      val mockUserAuthority = mock[UserAuthority]
      val saUtr = "someUtr"

      when(mockRequest.session).thenReturn(mockSession)
      when(mockSession.get("affinityGroup")).thenReturn(None)
      when(mockUser.userAuthority).thenReturn(mockUserAuthority)
      when(mockUserAuthority.saUtr).thenReturn(Some(SaUtr(saUtr)))

      val portalUrlBuilder = PortalDestinationUrlBuilder.build(mockRequest, mockUser) _
      evaluating(portalUrlBuilder("anotherDestinationPathKey")) should produce[RuntimeException]
    }

    "return a URL without the UTR resolved when the utr is missing" in new WithApplication(FakeApplication(additionalConfiguration = mockConfigValues)) {
      val mockRequest = mock[Request[AnyRef]]
      val mockSession = mock[Session]
      val mockUser = mock[User]
      val mockUserAuthority = mock[UserAuthority]

      when(mockRequest.session).thenReturn(mockSession)
      when(mockSession.get("affinityGroup")).thenReturn(Some(encrypt("someaffinitygroup")))
      when(mockUser.userAuthority).thenReturn(mockUserAuthority)
      when(mockUserAuthority.saUtr).thenReturn(None)

      val portalUrlBuilder = PortalDestinationUrlBuilder.build(mockRequest, mockUser) _
      val actualDestinationUrl = portalUrlBuilder("anotherDestinationPathKey")

      actualDestinationUrl should startWith("""http://someserver:8080/utr/<utr>/affinitygroup/someaffinitygroup/year""")
      actualDestinationUrl should not endWith ("""<year>""")
    }

    "return a URL which is resolved using a vrn parameter" in new WithApplication(FakeApplication(additionalConfiguration = mockConfigValues)) {
      val mockRequest = mock[Request[AnyRef]]
      val mockSession = mock[Session]
      val mockUser = mock[User]
      val mockUserAuthority = mock[UserAuthority]
      val vrn = "someVrn"

      when(mockRequest.session).thenReturn(mockSession)
      when(mockSession.get("affinityGroup")).thenReturn(Some(encrypt("someaffinitygroup")))
      when(mockUser.userAuthority).thenReturn(mockUserAuthority)
      when(mockUserAuthority.vrn).thenReturn(Some(Vrn(vrn)))

      val portalUrlBuilder = PortalDestinationUrlBuilder.build(mockRequest, mockUser) _
      val actualDestinationUrl = portalUrlBuilder("testVatAccountDetails")

      actualDestinationUrl should startWith("""http://someserver:8080/vat/trader/someVrn/account""")
    }

    "return an invalid URL when we request a link requiring a vrn parameter but the vrn is missing" in new WithApplication(FakeApplication(additionalConfiguration = mockConfigValues)) {
      val mockRequest = mock[Request[AnyRef]]
      val mockSession = mock[Session]
      val mockUser = mock[User]
      val mockUserAuthority = mock[UserAuthority]
      val vrn = "someVrn"

      when(mockRequest.session).thenReturn(mockSession)
      when(mockSession.get("affinityGroup")).thenReturn(Some(encrypt("someaffinitygroup")))
      when(mockUser.userAuthority).thenReturn(mockUserAuthority)
      when(mockUserAuthority.vrn).thenReturn(None)

      val portalUrlBuilder = PortalDestinationUrlBuilder.build(mockRequest, mockUser) _
      val actualDestinationUrl = portalUrlBuilder("testVatAccountDetails")

      actualDestinationUrl should startWith("""http://someserver:8080/vat/trader/<vrn>/account""")
    }


  }
}
