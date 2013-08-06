package controllers

import test.BaseSpec
import org.scalatest.matchers.ShouldMatchers
import play.api.test.{ FakeRequest, FakeApplication, WithApplication }
import play.api.mvc._
import org.joda.time.{ DateTimeZone, DateTime }
import play.api.test.Helpers._
import SessionTimeoutWrapper._

class SessionTimeoutWrapperSpec extends BaseSpec with ShouldMatchers {

  val homepageLocation = "/"
  val hypotheticalCurrentTime: DateTime = new DateTime(2012, 7, 7, 4, 6, 20, DateTimeZone.UTC)
  val invalidTime = hypotheticalCurrentTime.minusDays(1).getMillis.toString
  val justInvalidTime = hypotheticalCurrentTime.minusSeconds(timeoutSeconds + 1).getMillis.toString
  val justValidTime = hypotheticalCurrentTime.minusSeconds(timeoutSeconds - 1).getMillis.toString
  val validTime = hypotheticalCurrentTime.minusSeconds(1).getMillis.toString

  object TestController extends Controller with SessionTimeoutWrapper {
    override def now: () => DateTime = () => {
      hypotheticalCurrentTime
    }

    def testWithNewSessionTimeout = WithNewSessionTimeout(Action {
      request =>
        Ok("")
    })

    def testWithNewSessionTimeoutAddingData = WithNewSessionTimeout(Action {
      request =>
        Ok("").withSession("userId" -> "Jim")
    })

    def testWithSessionTimeoutValidation = WithSessionTimeoutValidation(Action {
      request =>
        Ok("").withSession("userId" -> "Tim")
    })

    def testValidateSession = ValidateSession()
  }

  import TestController.now

  "WithNewSessionTimeout" should {
    "add a timestamp to the session if the session is empty" in new WithApplication(FakeApplication()) {
      val result = TestController.testWithNewSessionTimeout(FakeRequest())
      session(result) mustBe Session(Map(sessionTimestampKey -> now().getMillis.toString))

    }
    "add a timestamp to the session but maintain the other values if the incoming session is not empty" in new WithApplication(FakeApplication()) {
      val result = TestController.testWithNewSessionTimeout(FakeRequest().withSession("userId" -> "Bob"))
      session(result) mustBe Session(Map(sessionTimestampKey -> now().getMillis.toString, "userId" -> "Bob"))
    }

    "add a timestamp to the session but maintain other values which have been added to the session overwriting request values" in new WithApplication(FakeApplication()) {
      val result = TestController.testWithNewSessionTimeoutAddingData(FakeRequest().withSession("userId" -> "Bob"))
      session(result) mustBe Session(Map(sessionTimestampKey -> now().getMillis.toString, "userId" -> "Jim"))
    }

  }

  "WithSessionTimeoutValidation" should {
    "redirect to the home page with a new session containing only a timestamp if the incoming session is empty" in new WithApplication(FakeApplication()) {
      val result = TestController.testWithSessionTimeoutValidation(FakeRequest())
      session(result) mustBe Session(Map(sessionTimestampKey -> now().getMillis.toString))
      redirectLocation(result) mustBe Some(homepageLocation)
    }
    "redirect to the home page with a new session containing only a timestamp if the incoming timestamp is invalid" in new WithApplication(FakeApplication()) {
      val result = TestController.testWithSessionTimeoutValidation(FakeRequest().withSession(sessionTimestampKey -> invalidTime))
      session(result) mustBe Session(Map(sessionTimestampKey -> now().getMillis.toString))
      redirectLocation(result) mustBe Some(homepageLocation)
    }

    "redirect to the home page with a new session containing only a timestamp if the incoming timestamp is just invalid" in new WithApplication(FakeApplication()) {
      val result = TestController.testWithSessionTimeoutValidation(FakeRequest().withSession(sessionTimestampKey -> justInvalidTime))
      session(result) mustBe Session(Map(sessionTimestampKey -> now().getMillis.toString))
      redirectLocation(result) mustBe Some(homepageLocation)
    }

    "perform the wrapped action successfully and update the timestamp if the incoming timestamp is just valid" in new WithApplication(FakeApplication()) {
      val result = TestController.testWithSessionTimeoutValidation(FakeRequest().withSession(sessionTimestampKey -> justValidTime))
      session(result) mustBe Session(Map(sessionTimestampKey -> now().getMillis.toString, "userId" -> "Tim"))
      status(result) mustBe 200
    }
    "perform the wrapped action successfully and update the timestamp if the incoming timestamp is valid" in new WithApplication(FakeApplication()) {
      val result = TestController.testWithSessionTimeoutValidation(FakeRequest().withSession(sessionTimestampKey -> validTime))
      session(result) mustBe Session(Map(sessionTimestampKey -> now().getMillis.toString, "userId" -> "Tim"))
      status(result) mustBe 200
    }
  }

  "validateSession" should {
    "return unauthorised if the incoming session is empty" in new WithApplication(FakeApplication()) {
      val result = TestController.testValidateSession(FakeRequest())
      status(result) mustBe 401
    }
    "return unauthorised if the incoming timestamp is invalid" in new WithApplication(FakeApplication()) {
      val result = TestController.testValidateSession(FakeRequest().withSession(sessionTimestampKey -> invalidTime))
      status(result) mustBe 401
    }

    "return unauthorised if the incoming timestamp is just invalid" in new WithApplication(FakeApplication()) {
      val result = TestController.testValidateSession(FakeRequest().withSession(sessionTimestampKey -> justInvalidTime))
      status(result) mustBe 401
    }

    "return ok if the incoming timestamp is just valid" in new WithApplication(FakeApplication()) {
      val result = TestController.testValidateSession(FakeRequest().withSession(sessionTimestampKey -> justValidTime))
      status(result) mustBe 200
    }
    "return ok if the incoming timestamp is valid" in new WithApplication(FakeApplication()) {
      val result = TestController.testValidateSession(FakeRequest().withSession(sessionTimestampKey -> validTime))
      status(result) mustBe 200
    }
  }

}
