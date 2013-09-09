package uk.gov.hmrc.common

import uk.gov.hmrc.microservice.domain.User
import uk.gov.hmrc.microservice.auth.domain.{ Utr, UserAuthority }
import org.scalatest.mock.MockitoSugar
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.test.{ FakeApplication, WithApplication }

class PortalDestinationUrlBuilderSpec extends BaseSpec with MockitoSugar {

  val mockConfigValues = Map("govuk-tax.Test.portal.destinationPath.someDestinationPathKey" -> "/utr/<utr>/year/<year>",
    "govuk-tax.Test.portal.destinationPath.anotherDestinationPathKey" -> "/utr/<utr>/affinitygroup/<affinitygroup>/year/<year>",
    "govuk-tax.Test.portal.destinationRoot" -> "http://someserver:8080")

  "PortalDestinationUrlBuilder " should {
    "return a resolved dynamic full URL with parameters year and utr resolved using a User object" ignore new WithApplication(FakeApplication(additionalConfiguration = mockConfigValues)) {
      val mockUser = mock[User]
      val mockUserAuthority = mock[UserAuthority]
      val utr = "someUtr"

      when(mockUser.userAuthority).thenReturn(mockUserAuthority)
      when(mockUserAuthority.utr).thenReturn(Some(Utr(utr)))

      val actualDestinationUrl = PortalDestinationUrlBuilder.buildUrl("someDestinationPathKey", mockUser)

      actualDestinationUrl should startWith("""http://someserver:8080/utr/someUtr/year""")
      actualDestinationUrl should not endWith ("""<year>""")
    }

    "return a full URL with parameter utr not resolved when the UTR is None" ignore new WithApplication(FakeApplication(additionalConfiguration = mockConfigValues)) {
      val actualDestinationUrl = PortalDestinationUrlBuilder.buildUrl("someDestinationPathKey", None)

      actualDestinationUrl should startWith("""http://someserver:8080/utr/<utr>/year""")
      actualDestinationUrl should not endWith ("""<year>""")
    }

    "return a resolved dynamic full URL with parameters year and utr resolved using utr parameter" ignore new WithApplication(FakeApplication(additionalConfiguration = mockConfigValues)) {
      val utr = "someUtr"

      val actualDestinationUrl = PortalDestinationUrlBuilder.buildUrl("someDestinationPathKey", Some(Utr(utr)))

      actualDestinationUrl should startWith("""http://someserver:8080/utr/someUtr/year""")
      actualDestinationUrl should not endWith ("""<year>""")
    }

    //TODO: This is a placeholder test for the affinity group placeholder
    "return a URL with placeholders for properties not recognised" ignore new WithApplication(FakeApplication(additionalConfiguration = mockConfigValues)) {
      val mockUser = mock[User]
      val mockUserAuthority = mock[UserAuthority]
      val utr = "someUtr"

      when(mockUser.userAuthority).thenReturn(mockUserAuthority)
      when(mockUserAuthority.utr).thenReturn(Some(Utr(utr)))

      val actualDestinationUrl = PortalDestinationUrlBuilder.buildUrl("anotherDestinationPathKey", mockUser)

      actualDestinationUrl should startWith("""http://someserver:8080/utr/someUtr/affinitygroup/<affinitygroup>/year""")
      actualDestinationUrl should not endWith ("""<year>""")
    }
  }
}
