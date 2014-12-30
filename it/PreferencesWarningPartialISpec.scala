

class PreferencesWarningPartialISpec
  extends PreferencesFrontEndServer
  with UserAuthentication {

  "partial html" should {
    "return not authorised when no credentials supplied" in new TestCase {
      `/account/preferences/warnings`.get() should have(status(401))
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
  }

  "partial html for verification pending" should {

    "have a verification warning for the unverified email" in new TestCase {
      val email = uniqueEmail
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))

      val response =`/account/preferences/warnings`.withHeaders(authenticationCookie(userId, password)).get()

      response should have(status(200))
      response.futureValue.allHeaders should contain("X-Opted-In-Email" -> Seq("true"))
      response.futureValue.body should include(s"Verify your Self Assessment email address")
    }

    "have no warning if user then verifies email" in new TestCase {
      val email = uniqueEmail
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))
      `/preferences-admin/sa/individual`.verifyEmailFor(utr) should have(status(204))

      val response =`/account/preferences/warnings`.withHeaders(authenticationCookie(userId, password)).get()

      response should have(status(200))
      response.futureValue.allHeaders should contain("X-Opted-In-Email" -> Seq("true"))
      response.futureValue.body should be("")
    }

    "have no warning if user then opts out" in new TestCase {
      val email = uniqueEmail
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))
      `/portal/preferences/sa/individual`.postOptOut(utr) should have(status(201))

      val response =`/account/preferences/warnings`.withHeaders(authenticationCookie(userId, password)).get()

      response should have(status(200))
      response.futureValue.allHeaders should contain("X-Opted-In-Email" -> Seq("false"))
      response.futureValue.body should be("")
    }

    "have verification warning if user then changes email" in new TestCase {
      val email = uniqueEmail
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))
      `/portal/preferences/sa/individual`.postPendingEmail(utr, changedUniqueEmail) should have(status(201))

      val response =`/account/preferences/warnings`.withHeaders(authenticationCookie(userId, password)).get()

      response should have(status(200))
      response.futureValue.allHeaders should contain("X-Opted-In-Email" -> Seq("true"))
      response.futureValue.body should include("Verify your Self Assessment email address")
    }

    "be not found if user is then de-enrolled" in new TestCase {
      val email = uniqueEmail
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))
      `/portal/preferences/sa/individual`.postDeEnrolling(utr)

      val response =`/account/preferences/warnings`.withHeaders(authenticationCookie(userId, password)).get()

      response should have(status(404))
    }
  }

  "partial html for a bounced unverified email address" should {

    "have a bounced warning" in new TestCase {
      val email = uniqueEmail
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))
      `/preferences-admin/sa/bounce-email`.post(email) should have(status(204))

      val response =`/account/preferences/warnings`.withHeaders(authenticationCookie(userId, password)).get()

      response should have(status(200))
      response.futureValue.allHeaders should contain("X-Opted-In-Email" -> Seq("true"))
      response.futureValue.body should include("There’s a problem with your Self Assessment email reminders")
    }

    "have no warning if user then opts out" in new TestCase {
      val email = uniqueEmail
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))
      `/preferences-admin/sa/bounce-email`.post(email) should have(status(204))
      `/portal/preferences/sa/individual`.postOptOut(utr)

      val response =`/account/preferences/warnings`.withHeaders(authenticationCookie(userId, password)).get()

      response should have(status(200))
      response.futureValue.allHeaders should contain("X-Opted-In-Email" -> Seq("false"))
      response.futureValue.body should be("")
    }

    "have a verification warning if user then successfully sends verification link to same address" in new TestCase {
      val email = uniqueEmail
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))
      `/preferences-admin/sa/bounce-email`.post(email) should have(status(204))
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))

      val response =`/account/preferences/warnings`.withHeaders(authenticationCookie(userId, password)).get()

      response should have(status(200))
      response.futureValue.allHeaders should contain("X-Opted-In-Email" -> Seq("true"))
      response.futureValue.body should include("Verify your Self Assessment email address")
    }

    "have verification warning if user then changes email" in new TestCase {
      val email = uniqueEmail
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))
      `/preferences-admin/sa/bounce-email`.post(email) should have(status(204))
      `/portal/preferences/sa/individual`.postPendingEmail(utr, changedUniqueEmail) should have(status(201))

      val response =`/account/preferences/warnings`.withHeaders(authenticationCookie(userId, password)).get()

      response should have(status(200))
      response.futureValue.allHeaders should contain("X-Opted-In-Email" -> Seq("true"))
      response.futureValue.body should include("Verify your Self Assessment email address")
    }

    "be not found if user is then de-enrolled" in new TestCase {
      val email = uniqueEmail
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))
      `/preferences-admin/sa/bounce-email`.post(email) should have(status(204))
      `/portal/preferences/sa/individual`.postDeEnrolling(utr) should have(status(201))

      val response =`/account/preferences/warnings`.withHeaders(authenticationCookie(userId, password)).get()

      response should have(status(404))
    }

    "have no warning if user is then de-enrolled followed by a re-enrol" in new TestCase {
      val email = uniqueEmail
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))
      `/preferences-admin/sa/bounce-email`.post(email) should have(status(204))
      `/portal/preferences/sa/individual`.postDeEnrolling(utr) should have(status(201))
      `/portal/preferences/sa/individual`.postOptOut(utr) should have(status(201))

      val response =`/account/preferences/warnings`.withHeaders(authenticationCookie(userId, password)).get()

      response should have(status(200))
      response.futureValue.body should be("")
    }

    "have no warning if user successfully resends link and verifies" in new TestCase {
      val email = uniqueEmail
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))
      `/preferences-admin/sa/bounce-email`.post(email) should have(status(204))
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))
      `/preferences-admin/sa/individual`.verifyEmailFor(utr) should have(status(204))

      val response =`/account/preferences/warnings`.withHeaders(authenticationCookie(userId, password)).get()

      response should have(status(200))
      response.futureValue.body should be("")
    }

  }

  "partial html for opted out user" should {

    "be empty" in new TestCase {
      `/portal/preferences/sa/individual`.postOptOut(utr) should have(status(201))

      val response = `/account/preferences/warnings`.withHeaders(authenticationCookie(userId, password)).get()

      response should have(status(200))
      response.futureValue.allHeaders should contain("X-Opted-In-Email" -> Seq("false"))
    }
  }

  "partial html for a bounced verified email address" should {

    "have a bounced warning" in new TestCase {
      val email = uniqueEmail
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))
      `/preferences-admin/sa/individual`.verifyEmailFor(utr) should have(status(204))
      `/preferences-admin/sa/bounce-email`.post(email) should have(status(204))

      val response = `/account/preferences/warnings`.withHeaders(authenticationCookie(userId, password)).get()

      response should have(status(200))
      response.futureValue.allHeaders should contain("X-Opted-In-Email" -> Seq("true"))
      response.futureValue.body should include(s"There’s a problem with your Self Assessment email reminders")
    }
  }
}