package uk.gov.hmrc.common

import uk.gov.hmrc.microservice.domain.User
import uk.gov.hmrc.microservice.auth.domain.{ Utr, UserAuthority }
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import play.api.test.{ FakeApplication, WithApplication }
import play.api.mvc.{ Session, Request }
import controllers.common.CookieEncryption

class PortalDestinationUrlBuilderSpec extends BaseSpec with MockitoSugar with CookieEncryption {

  val mockConfigValues = Map("govuk-tax.Test.portal.destinationPath.someDestinationPathKey" -> "/utr/<utr>/year/<year>",
    "govuk-tax.Test.portal.destinationPath.anotherDestinationPathKey" -> "/utr/<utr>/affinitygroup/<affinitygroup>/year/<year>",
    "govuk-tax.Test.portal.destinationRoot" -> "http://someserver:8080",
    "cookie.encryption.key" -> "gvBoGdgzqG1AarzF1LY0zQ=="
  )

  "PortalDestinationUrlBuilder " should {

    "return a resolved dynamic full URL with parameters year, utr and affinity group resolved using a request and user object" ignore new WithApplication(FakeApplication(additionalConfiguration = mockConfigValues)) {
      val mockRequest = mock[Request[AnyRef]]
      val mockSession = mock[Session]
      val mockUser = mock[User]
      val mockUserAuthority = mock[UserAuthority]
      val utr = "someUtr"

      when(mockRequest.session).thenReturn(mockSession)
      when(mockSession.get("affinityGroup")).thenReturn(Some(encrypt("someaffinitygroup")))
      when(mockUser.userAuthority).thenReturn(mockUserAuthority)
      when(mockUserAuthority.utr).thenReturn(Some(Utr(utr)))

      val portalUrlBuilder = PortalDestinationUrlBuilder.build(mockRequest, mockUser) _
      val actualDestinationUrl = portalUrlBuilder("anotherDestinationPathKey")

      actualDestinationUrl should startWith("""http://someserver:8080/utr/someUtr/affinitygroup/someaffinitygroup/year""")
      actualDestinationUrl should not endWith ("""<year>""")
    }

    "throw an exception when the affinity group is missing" ignore new WithApplication(FakeApplication(additionalConfiguration = mockConfigValues)) {
      val mockRequest = mock[Request[AnyRef]]
      val mockSession = mock[Session]
      val mockUser = mock[User]
      val mockUserAuthority = mock[UserAuthority]
      val utr = "someUtr"

      when(mockRequest.session).thenReturn(mockSession)
      when(mockSession.get("affinityGroup")).thenReturn(None)
      when(mockUser.userAuthority).thenReturn(mockUserAuthority)
      when(mockUserAuthority.utr).thenReturn(Some(Utr(utr)))

      val portalUrlBuilder = PortalDestinationUrlBuilder.build(mockRequest, mockUser) _
      evaluating(portalUrlBuilder("anotherDestinationPathKey")) should produce[RuntimeException]
    }

    "return a URL without the UTR resolved when the utr is missing" ignore new WithApplication(FakeApplication(additionalConfiguration = mockConfigValues)) {
      val mockRequest = mock[Request[AnyRef]]
      val mockSession = mock[Session]
      val mockUser = mock[User]
      val mockUserAuthority = mock[UserAuthority]

      when(mockRequest.session).thenReturn(mockSession)
      when(mockSession.get("affinityGroup")).thenReturn(Some(encrypt("someaffinitygroup")))
      when(mockUser.userAuthority).thenReturn(mockUserAuthority)
      when(mockUserAuthority.utr).thenReturn(None)

      val portalUrlBuilder = PortalDestinationUrlBuilder.build(mockRequest, mockUser) _
      val actualDestinationUrl = portalUrlBuilder("anotherDestinationPathKey")

      actualDestinationUrl should startWith("""http://someserver:8080/utr/<utr>/affinitygroup/someaffinitygroup/year""")
      actualDestinationUrl should not endWith ("""<year>""")
    }
  }
}
