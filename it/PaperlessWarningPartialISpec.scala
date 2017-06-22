import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.play.http.test.ResponseMatchers

class PaperlessWarningPartialISpec
  extends PreferencesFrontEndServer
  with BeforeAndAfterEach
  with ResponseMatchers {

  "Paperless warnings partial " should {
    "return not authorised when no credentials supplied" in new TestCase {
      `/paperless/warnings`.get() should have(status(401))
    }

    "be not found if the user has no preferences with utr only" in new TestCaseWithFrontEndAuthentication {
      val response = `/paperless/warnings`.withHeaders(cookieWithUtr).get()

      response should have(status(404))
    }

    "be not found if the user has no preferences with nino only" in new TestCaseWithFrontEndAuthentication {
      val response = `/paperless/warnings`.withHeaders(cookieWithNino).get()

      response should have(status(404))
    }
  }

  "Paperless warnings partial for verification pending" should {

    "have a verification warning for the unverified email" in new TestCaseWithFrontEndAuthentication {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email) should have (status(201))

      val response =`/paperless/warnings`.withHeaders(cookieWithUtr).get()

      response should have(status(200))
      response.futureValue.body should include(s"Verify your email address for paperless notifications")
    }

    "have no warning if user then verifies email" in new TestCaseWithFrontEndAuthentication {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email) should have(status(201))
      `/preferences-admin/sa/individual`.verifyEmailFor(`/entity-resolver/sa/:utr`(utr.value)) should have(status(204))

      val response =`/paperless/warnings`.withHeaders(cookieWithUtr).get()

      response should have(status(200))
      response.futureValue.body should be("")
    }

    "have no warning if user then opts out" in new TestCaseWithFrontEndAuthentication {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email) should have(status(201))
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptOut should have(status(200))

      val response =`/paperless/warnings`.withHeaders(cookieWithUtr).get()

      response should have(status(200))
      response.futureValue.body should be("")
    }

    "have verification warning if user then changes email" in new TestCaseWithFrontEndAuthentication {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email) should have(status(201))
      `/preferences`(ggAuthHeaderWithUtr).putPendingEmail(changedUniqueEmail) should have(status(200))

      val response =`/paperless/warnings`.withHeaders(cookieWithUtr).get()

      response should have(status(200))
      response.futureValue.body should include("Verify your email address for paperless notifications")
    }
  }

  "Paperless warnings partial for a bounced unverified email address" should {

    "have a bounced warning" in new TestCaseWithFrontEndAuthentication {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email) should have(status(201))
      `/preferences-admin/bounce-email`.post(email) should have(status(204))

      val response =`/paperless/warnings`.withHeaders(cookieWithUtr).get()

      response should have(status(200))
      response.futureValue.body should include("There's a problem with your paperless notification emails")
    }

    "have no warning if user then opts out" in new TestCaseWithFrontEndAuthentication {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email) should have(status(201))
      `/preferences-admin/bounce-email`.post(email) should have(status(204))
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptOut

      val response =`/paperless/warnings`.withHeaders(cookieWithUtr).get()

      response should have(status(200))
      response.futureValue.body should be("")
    }

    "have a verification warning if user then successfully sends verification link to same address" in new TestCaseWithFrontEndAuthentication {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email) should have(status(201))
      `/preferences-admin/bounce-email`.post(email) should have(status(204))
      `/preferences`(ggAuthHeaderWithUtr).putPendingEmail(email) should have(status(200))

      val response =`/paperless/warnings`.withHeaders(cookieWithUtr).get()

      response should have(status(200))
      response.futureValue.body should include("Verify your email address for paperless notifications")
    }

    "have verification warning if user then changes email" in new TestCaseWithFrontEndAuthentication {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email) should have(status(201))
      `/preferences-admin/bounce-email`.post(email) should have(status(204))
      `/preferences`(ggAuthHeaderWithUtr).putPendingEmail(changedUniqueEmail) should have(status(200))

      val response =`/paperless/warnings`.withHeaders(cookieWithUtr).get()

      response should have(status(200))
      response.futureValue.body should include("Verify your email address for paperless notifications")
    }

    "have no warning if user successfully resends link and verifies" in new TestCaseWithFrontEndAuthentication {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email) should have(status(201))
      `/preferences-admin/bounce-email`.post(email) should have(status(204))
      `/preferences`(ggAuthHeaderWithUtr).putPendingEmail(email) should have(status(200))
      `/preferences-admin/sa/individual`.verifyEmailFor(`/entity-resolver/sa/:utr`(utr.value)) should have(status(204))

      val response =`/paperless/warnings`.withHeaders(cookieWithUtr).get()

      response should have(status(200))
      response.futureValue.body should be("")
    }

    "have inbox full warning if user resends link and their inbox is full" in new TestCaseWithFrontEndAuthentication {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email) should have(status(201))
      `/preferences-admin/bounce-email`.post(email) should have(status(204))
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email) should have(status(200))
      `/preferences-admin/sa/bounce-email-inbox-full`.post(email) should have(status(204))

      val response =`/paperless/warnings`.withHeaders(cookieWithUtr).get()

      response should have(status(200))
      response.futureValue.body should include("Your inbox is full")
    }

  }

  "Paperless warnings partial for opted out user" should {

    "be empty" in new TestCaseWithFrontEndAuthentication {
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptOut should have(status(201))

      val response = `/paperless/warnings`.withHeaders(cookieWithUtr).get()

      response should have(status(200))
    }
  }

  "Paperless warnings partial for a bounced verified email address" should {

    "have a bounced warning" in new TestCaseWithFrontEndAuthentication {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email) should have(status(201))
      `/preferences-admin/sa/individual`.verifyEmailFor(`/entity-resolver/sa/:utr`(utr.value)) should have(status(204))
      `/preferences-admin/bounce-email`.post(email) should have(status(204))

      val response = `/paperless/warnings`.withHeaders(cookieWithUtr).get()

      response should have(status(200))
      response.futureValue.body should include("There's a problem with your paperless notification emails")
    }
  }
}
