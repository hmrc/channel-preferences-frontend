

class PreferencesWarningPartialISpec
  extends PreferencesFrontEndServer
  with UserAuthentication {

  "partial html for pending verification email" should {

    "return not authorised when no credentials supplied" in new TestCase {
      `/account/preferences/warnings`.get() should have(status(401))
    }

    "be empty if the user has opted out" in new TestCase {
      `/portal/preferences/sa/individual`.postOptOut(utr) should have(status(201))

      val response = `/account/preferences/warnings`.withHeaders(authenticationCookie(userId, password)).get()

      response should have(status(200))
    }

    "be not found if the user has no preferences" in new TestCase {
      `/preferences-admin/sa/individual`.delete(utr) should have(status(200))

      val response = `/account/preferences/warnings`.withHeaders(authenticationCookie(userId, password)).get()

      response should have(status(404))
    }

    "be not found if the user is de-enrolled" in new TestCase {
      val email = uniqueEmail
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))
      `/portal/preferences/sa/individual`.postDeEnrolling(utr) should have(status(201))

      val response = `/account/preferences/warnings`.withHeaders(authenticationCookie(userId, password)).get()

      response should have(status(404))
    }

    "have no warning for a verified email" in new TestCase {
      val email = uniqueEmail
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))
      `/preferences-admin/sa/individual`.verifyEmailFor(utr) should have(status(204))

      val response =`/account/preferences/warnings`.withHeaders(authenticationCookie(userId, password)).get()

      response should have(status(200))
    }

    "have warning for a pending unverified email" in new TestCase {
      val email = uniqueEmail
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))

      val response =`/account/preferences/warnings`.withHeaders(authenticationCookie(userId, password)).get()

      response should have(status(200))
    }

    "have warning for a bounced and unverified pending email address" in new TestCase {
      val email = uniqueEmail
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))
      `/preferences-admin/sa/bounce-email`.post(email) should have(status(204))

      val response =`/account/preferences/warnings`.withHeaders(authenticationCookie(userId, password)).get()

      response should have(status(200))
    }

    "have warning for a bounced and verified email address" in new TestCase {
      val email = uniqueEmail
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))
      `/preferences-admin/sa/individual`.verifyEmailFor(utr) should have(status(204))
      `/preferences-admin/sa/bounce-email`.post(email) should have(status(204))

      val response =`/account/preferences/warnings`.withHeaders(authenticationCookie(userId, password)).get()

      response should have(status(200))
    }
  }
}
