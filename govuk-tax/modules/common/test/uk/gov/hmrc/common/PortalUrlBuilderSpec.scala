package uk.gov.hmrc.common

import uk.gov.hmrc.common.microservice.domain.User
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import play.api.test.{FakeRequest, FakeApplication, WithApplication}
import play.api.mvc.{ Session, Request }
import controllers.common.SessionKeys

class PortalUrlBuilderSpec extends BaseSpec with MockitoSugar {

  import controllers.domain.AuthorityUtils._

  val testConfig = Map(
    "govuk-tax.Test.portal.destinationPath.someDestinationPathKey" -> "http://someserver:8080/utr/<utr>/year/<year>",
    "govuk-tax.Test.portal.destinationPath.anotherDestinationPathKey" -> "http://someserver:8080/utr/<utr>/affinitygroup/<affinitygroup>/year/<year>",
    "govuk-tax.Test.portal.destinationPath.testVatAccountDetails" -> "http://someserver:8080/vat/trader/<vrn>/account",
    "govuk-tax.Test.portal.destinationPath.testCtAccountDetails" -> "http://someserver:8080/corporation-tax/org/<ctutr>/account",
    "govuk-tax.Test.portal.destinationPath.testEmpRefAccountDetails" -> "http://someserver:8080/corporation-tax/empref/<empref>/account",
    "govuk-tax.Test.portal.destinationRoot" -> "http://someOtherserver:8080",
    "cookie.encryption.key" -> "gvBoGdgzqG1AarzF1LY0zQ=="
  )

  trait Setup {
    def session: Seq[(String, String)] = Seq((SessionKeys.affinityGroup -> "someaffinitygroup"))
    implicit lazy val mockRequest = FakeRequest().withSession(session: _*)
    implicit val mockUser = mock[User]

    val portalUrlBuilder = new PortalUrlBuilder {}
  }

  "PortalUrlBuilder " should {

    "return a resolved dynamic full URL with parameters year and saUtr resolved using a request and user object" in new WithApplication(FakeApplication(additionalConfiguration = testConfig)) with Setup {
      when(mockUser.userAuthority).thenReturn(saAuthority("123", "someUtr"))

      val actualDestinationUrl = portalUrlBuilder.buildPortalUrl("anotherDestinationPathKey")(mockRequest, mockUser)

      actualDestinationUrl should startWith("""http://someserver:8080/utr/someUtr/affinitygroup/someaffinitygroup/year""")
      actualDestinationUrl should not endWith """<year>"""
    }

    "return a URL without the UTR resolved when the utr is missing" in new WithApplication(FakeApplication(additionalConfiguration = testConfig)) with Setup {
      when(mockUser.userAuthority).thenReturn(payeAuthority("", "someNino"))

      val actualDestinationUrl = portalUrlBuilder.buildPortalUrl("anotherDestinationPathKey")

      actualDestinationUrl should startWith("""http://someserver:8080/utr/<utr>/affinitygroup/someaffinitygroup/year""")
      actualDestinationUrl should not endWith """<year>"""
    }

    "return a URL which is resolved using a vrn parameter" in new WithApplication(FakeApplication(additionalConfiguration = testConfig)) with Setup {
      when(mockUser.userAuthority).thenReturn(vatAuthority("", "someVrn"))
      portalUrlBuilder.buildPortalUrl("testVatAccountDetails") should startWith("""http://someserver:8080/vat/trader/someVrn/account""")
    }

    "return an invalid URL when we request a link requiring a vrn parameter but the vrn is missing" in new WithApplication(FakeApplication(additionalConfiguration = testConfig)) with Setup {
      when(mockUser.userAuthority).thenReturn(saAuthority("", ""))
      portalUrlBuilder.buildPortalUrl("testVatAccountDetails") should startWith("""http://someserver:8080/vat/trader/<vrn>/account""")
    }

    "return a URL which is resolved using a ctUtr parameter" in new WithApplication(FakeApplication(additionalConfiguration = testConfig)) with Setup {
      when(mockUser.userAuthority).thenReturn(ctAuthority("", "someCtUtr"))
      portalUrlBuilder.buildPortalUrl("testCtAccountDetails") should startWith("""http://someserver:8080/corporation-tax/org/someCtUtr/account""")
    }

    "return a URL which is resolved using the empRef parameter" in new WithApplication(FakeApplication(additionalConfiguration = testConfig)) with Setup {
      when(mockUser.userAuthority).thenReturn(epayeAuthority("", "some/EmpRef"))
      portalUrlBuilder.buildPortalUrl("testEmpRefAccountDetails") should startWith("""http://someserver:8080/corporation-tax/empref/some/EmpRef/account""")
    }

    "return an invalid URL when we request a link requiring a ctUtr parameter but the ctUtr is missing" in new WithApplication(FakeApplication(additionalConfiguration = testConfig)) with Setup {
      when(mockUser.userAuthority).thenReturn(vatAuthority("", "vrn"))
      portalUrlBuilder.buildPortalUrl("testCtAccountDetails") should startWith("""http://someserver:8080/corporation-tax/org/<ctutr>/account""")
    }

    "throw an exception when the affinity group is missing from the session" in new WithApplication(FakeApplication(additionalConfiguration = testConfig)) with Setup {
      override def session = Seq()
      evaluating(portalUrlBuilder.buildPortalUrl("anotherDestinationPathKey")) should produce[RuntimeException]
    }
  }
  "toSaTaxYearRepresentation" should {

    val portalUrlBuilder = new PortalUrlBuilder {}

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
