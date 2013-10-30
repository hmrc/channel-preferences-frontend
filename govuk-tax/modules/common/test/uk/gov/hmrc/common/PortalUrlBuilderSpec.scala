package uk.gov.hmrc.common

import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import play.api.test.{ FakeApplication, WithApplication }
import play.api.mvc.{ Session, Request }
import uk.gov.hmrc.domain.{CtUtr, Vrn, SaUtr}

class PortalUrlBuilderSpec extends BaseSpec with MockitoSugar {

  val mockAffinityGroupParser = mock[AffinityGroupParser]

  val portalUrlBuilder = new PortalUrlBuilder {
    override def parseAffinityGroup(implicit request: Request[AnyRef]): String = mockAffinityGroupParser.parseAffinityGroup
  }
  
  val mockConfigValues = Map(
    "govuk-tax.Test.portal.destinationPath.someDestinationPathKey" -> "http://someserver:8080/utr/<utr>/year/<year>",
    "govuk-tax.Test.portal.destinationPath.anotherDestinationPathKey" -> "http://someserver:8080/utr/<utr>/affinitygroup/<affinitygroup>/year/<year>",
    "govuk-tax.Test.portal.destinationPath.testVatAccountDetails" -> "http://someserver:8080/vat/trader/<vrn>/account",
    "govuk-tax.Test.portal.destinationPath.testCtAccountDetails" -> "http://someserver:8080/corporation-tax/org/<ctutr>/account",
    "govuk-tax.Test.portal.destinationRoot" -> "http://someOtherserver:8080",
    "cookie.encryption.key" -> "gvBoGdgzqG1AarzF1LY0zQ=="
  )
  
  "PortalUrlBuilder " should {

    "return a resolved dynamic full URL with parameters year, saUtr and affinity group resolved using a request and user object" in new WithApplication(FakeApplication(additionalConfiguration = mockConfigValues)) {
      implicit val mockRequest= mock[Request[AnyRef]]
      implicit val mockUser = mock[User]
      val mockUserAuthority = mock[UserAuthority]
      val saUtr = "someUtr"

      when(mockAffinityGroupParser.parseAffinityGroup).thenReturn("someaffinitygroup")
      when(mockUser.userAuthority).thenReturn(mockUserAuthority)
      when(mockUserAuthority.saUtr).thenReturn(Some(SaUtr(saUtr)))

      val actualDestinationUrl = portalUrlBuilder.buildPortalUrl("anotherDestinationPathKey")

      actualDestinationUrl should startWith("""http://someserver:8080/utr/someUtr/affinitygroup/someaffinitygroup/year""")
      actualDestinationUrl should not endWith """<year>"""
    }

    "throw an exception when the affinity group is missing" in new WithApplication(FakeApplication(additionalConfiguration = mockConfigValues)) {
      implicit val mockRequest= mock[Request[AnyRef]]
      implicit val mockUser = mock[User]
      val mockUserAuthority = mock[UserAuthority]
      val saUtr = "someUtr"

      when(mockAffinityGroupParser.parseAffinityGroup).thenThrow(new InternalError)
      when(mockUser.userAuthority).thenReturn(mockUserAuthority)
      when(mockUserAuthority.saUtr).thenReturn(Some(SaUtr(saUtr)))

      evaluating(portalUrlBuilder.buildPortalUrl("anotherDestinationPathKey")) should produce[InternalError]
    }

    "return a URL without the UTR resolved when the utr is missing" in new WithApplication(FakeApplication(additionalConfiguration = mockConfigValues)) {
      implicit val mockRequest= mock[Request[AnyRef]]
      implicit val mockUser = mock[User]
      val mockSession = mock[Session]
      val mockUserAuthority = mock[UserAuthority]

      when(mockAffinityGroupParser.parseAffinityGroup).thenReturn("someaffinitygroup")
      when(mockUser.userAuthority).thenReturn(mockUserAuthority)
      when(mockUserAuthority.saUtr).thenReturn(None)

      val actualDestinationUrl = portalUrlBuilder.buildPortalUrl("anotherDestinationPathKey")

      actualDestinationUrl should startWith("""http://someserver:8080/utr/<utr>/affinitygroup/someaffinitygroup/year""")
      actualDestinationUrl should not endWith """<year>"""
    }

    "return a URL which is resolved using a vrn parameter" in new WithApplication(FakeApplication(additionalConfiguration = mockConfigValues)) {
      implicit val mockRequest= mock[Request[AnyRef]]
      implicit val mockUser = mock[User]
      val mockUserAuthority = mock[UserAuthority]
      val vrn = "someVrn"

      when(mockAffinityGroupParser.parseAffinityGroup).thenReturn("someaffinitygroup")
      when(mockUser.userAuthority).thenReturn(mockUserAuthority)
      when(mockUserAuthority.vrn).thenReturn(Some(Vrn(vrn)))

      val actualDestinationUrl = portalUrlBuilder.buildPortalUrl("testVatAccountDetails")

      actualDestinationUrl should startWith("""http://someserver:8080/vat/trader/someVrn/account""")
    }

    "return an invalid URL when we request a link requiring a vrn parameter but the vrn is missing" in new WithApplication(FakeApplication(additionalConfiguration = mockConfigValues)) {
      implicit val mockRequest= mock[Request[AnyRef]]
      implicit val mockUser = mock[User]
      val mockUserAuthority = mock[UserAuthority]
      val vrn = "someVrn"

      when(mockAffinityGroupParser.parseAffinityGroup).thenReturn("someaffinitygroup")
      when(mockUser.userAuthority).thenReturn(mockUserAuthority)
      when(mockUserAuthority.vrn).thenReturn(None)

      val actualDestinationUrl = portalUrlBuilder.buildPortalUrl("testVatAccountDetails")

      actualDestinationUrl should startWith("""http://someserver:8080/vat/trader/<vrn>/account""")
    }

    "return a URL which is resolved using a ctUtr parameter" in new WithApplication(FakeApplication(additionalConfiguration = mockConfigValues)) {
      implicit val mockRequest= mock[Request[AnyRef]]
      implicit val mockUser = mock[User]
      val mockUserAuthority = mock[UserAuthority]
      val ctUtr = "someCtUtr"

      when(mockAffinityGroupParser.parseAffinityGroup).thenReturn("someaffinitygroup")
      when(mockUser.userAuthority).thenReturn(mockUserAuthority)
      when(mockUserAuthority.ctUtr).thenReturn(Some(CtUtr(ctUtr)))

      val actualDestinationUrl = portalUrlBuilder.buildPortalUrl("testCtAccountDetails")

      actualDestinationUrl should startWith("""http://someserver:8080/corporation-tax/org/someCtUtr/account""")
    }

    "return an invalid URL when we request a link requiring a ctUtr parameter but the ctUtr is missing" in new WithApplication(FakeApplication(additionalConfiguration = mockConfigValues)) {
      implicit val mockRequest= mock[Request[AnyRef]]
      implicit val mockUser = mock[User]
      val mockUserAuthority = mock[UserAuthority]
      val ctUtr = "someCtUtr"

      when(mockAffinityGroupParser.parseAffinityGroup).thenReturn("someaffinitygroup")
      when(mockUser.userAuthority).thenReturn(mockUserAuthority)
      when(mockUserAuthority.ctUtr).thenReturn(None)

      val actualDestinationUrl = portalUrlBuilder.buildPortalUrl("testCtAccountDetails")

      actualDestinationUrl should startWith("""http://someserver:8080/corporation-tax/org/<ctutr>/account""")
    }
  }
}
