package uk.gov.hmrc.common

import uk.gov.hmrc.common.microservice.domain.User
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import play.api.test.{ FakeApplication, WithApplication }
import play.api.mvc.{ Session, Request }

class PortalUrlBuilderSpec extends BaseSpec with MockitoSugar {

  import controllers.domain.AuthorityUtils._

  val mockAffinityGroupParser = mock[AffinityGroupParser]

  val portalUrlBuilder = new PortalUrlBuilder {
    override def parseAffinityGroup(implicit request: Request[AnyRef]): String = mockAffinityGroupParser.parseAffinityGroup
  }

  val testConfig = Map(
    "govuk-tax.Test.portal.destinationPath.someDestinationPathKey" -> "http://someserver:8080/utr/<utr>/year/<year>",
    "govuk-tax.Test.portal.destinationPath.anotherDestinationPathKey" -> "http://someserver:8080/utr/<utr>/affinitygroup/<affinitygroup>/year/<year>",
    "govuk-tax.Test.portal.destinationPath.testVatAccountDetails" -> "http://someserver:8080/vat/trader/<vrn>/account",
    "govuk-tax.Test.portal.destinationPath.testCtAccountDetails" -> "http://someserver:8080/corporation-tax/org/<ctutr>/account",
    "govuk-tax.Test.portal.destinationPath.testEmpRefAccountDetails" -> "http://someserver:8080/corporation-tax/empref/<empref>/account",
    "govuk-tax.Test.portal.destinationRoot" -> "http://someOtherserver:8080",
    "cookie.encryption.key" -> "gvBoGdgzqG1AarzF1LY0zQ=="
  )
  
  "PortalUrlBuilder " should {

    "return a resolved dynamic full URL with parameters year and saUtr resolved using a request and user object" in new WithApplication(FakeApplication(additionalConfiguration = testConfig)) {
      implicit val mockRequest= mock[Request[AnyRef]]
      implicit val mockUser = mock[User]
      val saUtr = "someUtr"

      when(mockUser.userAuthority).thenReturn(saAuthority("123", saUtr))
      when(mockAffinityGroupParser.parseAffinityGroup).thenReturn("someaffinitygroup")

      val actualDestinationUrl = portalUrlBuilder.buildPortalUrl("anotherDestinationPathKey")

      actualDestinationUrl should startWith("""http://someserver:8080/utr/someUtr/affinitygroup/someaffinitygroup/year""")
      actualDestinationUrl should not endWith """<year>"""
    }

    "return a URL without the UTR resolved when the utr is missing" in new WithApplication(FakeApplication(additionalConfiguration = testConfig)) {
      implicit val mockRequest= mock[Request[AnyRef]]
      implicit val mockUser = mock[User]
      val mockSession = mock[Session]

      when(mockUser.userAuthority).thenReturn(payeAuthority("", "someNino"))
      when(mockAffinityGroupParser.parseAffinityGroup).thenReturn("someaffinitygroup")

      val actualDestinationUrl = portalUrlBuilder.buildPortalUrl("anotherDestinationPathKey")

      actualDestinationUrl should startWith("""http://someserver:8080/utr/<utr>/affinitygroup/someaffinitygroup/year""")
      actualDestinationUrl should not endWith """<year>"""
    }

    "return a URL which is resolved using a vrn parameter" in new WithApplication(FakeApplication(additionalConfiguration = testConfig)) {
      implicit val mockRequest= mock[Request[AnyRef]]
      implicit val mockUser = mock[User]
      when(mockUser.userAuthority).thenReturn(vatAuthority("", "someVrn"))
      when(mockAffinityGroupParser.parseAffinityGroup).thenReturn("someaffinitygroup")

      val actualDestinationUrl = portalUrlBuilder.buildPortalUrl("testVatAccountDetails")

      actualDestinationUrl should startWith("""http://someserver:8080/vat/trader/someVrn/account""")
    }

    "return an invalid URL when we request a link requiring a vrn parameter but the vrn is missing" in new WithApplication(FakeApplication(additionalConfiguration = testConfig)) {
      implicit val mockRequest= mock[Request[AnyRef]]
      implicit val mockUser = mock[User]

      when(mockUser.userAuthority).thenReturn(saAuthority("", ""))
      when(mockAffinityGroupParser.parseAffinityGroup).thenReturn("someaffinitygroup")

      val actualDestinationUrl = portalUrlBuilder.buildPortalUrl("testVatAccountDetails")

      actualDestinationUrl should startWith("""http://someserver:8080/vat/trader/<vrn>/account""")
    }

    "return a URL which is resolved using a ctUtr parameter" in new WithApplication(FakeApplication(additionalConfiguration = testConfig)) {
      implicit val mockRequest= mock[Request[AnyRef]]
      implicit val mockUser = mock[User]
      when(mockUser.userAuthority).thenReturn(ctAuthority("", "someCtUtr"))
      when(mockAffinityGroupParser.parseAffinityGroup).thenReturn("someaffinitygroup")

      val actualDestinationUrl = portalUrlBuilder.buildPortalUrl("testCtAccountDetails")

      actualDestinationUrl should startWith("""http://someserver:8080/corporation-tax/org/someCtUtr/account""")
    }

    "return a URL which is resolved using the empRef parameter" in new WithApplication(FakeApplication(additionalConfiguration = testConfig)) {
      implicit val mockRequest= mock[Request[AnyRef]]
      implicit val mockUser = mock[User]
      when(mockUser.userAuthority).thenReturn(epayeAuthority("", "some/EmpRef"))
      when(mockAffinityGroupParser.parseAffinityGroup).thenReturn("someaffinitygroup")

      val actualDestinationUrl = portalUrlBuilder.buildPortalUrl("testEmpRefAccountDetails")

      actualDestinationUrl should startWith("""http://someserver:8080/corporation-tax/empref/some/EmpRef/account""")
    }

    "return an invalid URL when we request a link requiring a ctUtr parameter but the ctUtr is missing" in new WithApplication(FakeApplication(additionalConfiguration = testConfig)) {
      implicit val mockRequest= mock[Request[AnyRef]]
      implicit val mockUser = mock[User]
      val ctUtr = "someCtUtr"

      when(mockUser.userAuthority).thenReturn(vatAuthority("", "vrn"))
      when(mockAffinityGroupParser.parseAffinityGroup).thenReturn("someaffinitygroup")

      val actualDestinationUrl = portalUrlBuilder.buildPortalUrl("testCtAccountDetails")

      actualDestinationUrl should startWith("""http://someserver:8080/corporation-tax/org/<ctutr>/account""")
    }

    "throw an exception when the affinity group is missing" in new WithApplication(FakeApplication(additionalConfiguration = testConfig)) {
      implicit val mockRequest= mock[Request[AnyRef]]
      implicit val mockUser = mock[User]
      val saUtr = "someUtr"

      when(mockAffinityGroupParser.parseAffinityGroup).thenThrow(new RuntimeException)
      when(mockUser.userAuthority).thenReturn(saAuthority("", saUtr))


      evaluating(portalUrlBuilder.buildPortalUrl("anotherDestinationPathKey")) should produce[RuntimeException]
    }

  }
  "toSaTaxYearRepresentation" should {
    "return 1213 for the 2013 tax year" in {
      val result = portalUrlBuilder.toSaTaxYearRepresentation(2013)
      result shouldBe "1213"
    }
    "return 9900 for the 2000 tax year" in {
      val result = portalUrlBuilder.toSaTaxYearRepresentation(2000)
      result shouldBe "9900"
    }
    "return 0001 for the 2001 tax year" in {
      val result = portalUrlBuilder.toSaTaxYearRepresentation(2001)
      result shouldBe "0001"
    }
  }
}
