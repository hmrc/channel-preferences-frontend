package controllers

import play.api.test.{FakeRequest, FakeApplication, WithApplication}
import play.api.mvc._
import play.api.mvc.Results._
import org.joda.time.{DateTimeZone, DateTime}
import controllers.common._
import SessionTimeoutWrapper._
import uk.gov.hmrc.common.BaseSpec
import java.util.UUID

class SessionTimeoutWrapperSpec extends BaseSpec {

  import play.api.test.Helpers._

  val accountLoginPage = routes.LoginController.businessTaxLogin().url
  val hypotheticalCurrentTime = new DateTime(2012, 7, 7, 4, 6, 20, DateTimeZone.UTC)
  val invalidTime = hypotheticalCurrentTime.minusDays(1).getMillis.toString
  val justInvalidTime = hypotheticalCurrentTime.minusSeconds(timeoutSeconds + 1).getMillis.toString
  val justValidTime = hypotheticalCurrentTime.minusSeconds(timeoutSeconds - 1).getMillis.toString
  val validTime = hypotheticalCurrentTime.minusSeconds(1).getMillis.toString

  val now: () => DateTime = () => hypotheticalCurrentTime


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
        Ok("").withSession(SessionKeys.userId -> "Jim")
    })

    def testWithSessionTimeoutValidation = WithSessionTimeoutValidation(AnyAuthenticationProvider)(Action {
      request =>
        Ok("").withSession(SessionKeys.userId -> "Tim")
    })
  }


  "WithNewSessionTimeout" should {
    "add a timestamp to the session if the session is empty" in new WithApplication(FakeApplication()) {
      val result = TestController.testWithNewSessionTimeout(FakeRequest())
      session(result) shouldBe Session(Map(SessionKeys.lastRequestTimestamp -> now().getMillis.toString))

    }
    "add a timestamp to the session but maintain the other values if the incoming session is not empty" in new WithApplication(FakeApplication()) {
      val sessionId = s"session-${UUID.randomUUID}"
      val result = TestController.testWithNewSessionTimeout(FakeRequest().withSession(SessionKeys.userId -> sessionId))
      session(result) shouldBe Session(Map(SessionKeys.lastRequestTimestamp -> now().getMillis.toString, SessionKeys.userId -> sessionId))
    }

    "add a timestamp to the session but maintain other values which have been added to the session overwriting request values" in new WithApplication(FakeApplication()) {
      val sessionId = s"session-${UUID.randomUUID}"
      val result = TestController.testWithNewSessionTimeoutAddingData(FakeRequest().withSession(SessionKeys.userId -> sessionId))
      session(result) shouldBe Session(Map(SessionKeys.lastRequestTimestamp -> now().getMillis.toString, SessionKeys.userId -> "Jim"))
    }
  }

  "WithSessionTimeoutValidation" should {
    "redirect to the login page with a new session containing only a timestamp if the incoming session is empty" in new WithApplication(FakeApplication()) {
      val result = TestController.testWithSessionTimeoutValidation(FakeRequest())
      session(result) shouldBe Session(Map(SessionKeys.lastRequestTimestamp -> now().getMillis.toString))
      redirectLocation(result) shouldBe Some(accountLoginPage)
    }
    "redirect to the login page with a new session containing only a timestamp if the incoming timestamp is invalid" in new WithApplication(FakeApplication()) {
      val result = TestController.testWithSessionTimeoutValidation(FakeRequest().withSession(SessionKeys.lastRequestTimestamp -> invalidTime))
      session(result) shouldBe Session(Map(SessionKeys.lastRequestTimestamp -> now().getMillis.toString))
      redirectLocation(result) shouldBe Some(accountLoginPage)
    }

    "redirect to the login page with a new session containing only a timestamp if the incoming timestamp is just invalid" in new WithApplication(FakeApplication()) {
      val result = TestController.testWithSessionTimeoutValidation(FakeRequest().withSession(SessionKeys.lastRequestTimestamp -> justInvalidTime))
      session(result) shouldBe Session(Map(SessionKeys.lastRequestTimestamp -> now().getMillis.toString))
      redirectLocation(result) shouldBe Some(accountLoginPage)
    }



    "perform the wrapped action successfully and update the timestamp if the incoming timestamp is just valid when a custom error path is given" in new WithApplication(FakeApplication()) {
      val result = TestController.testWithSessionTimeoutValidation()(FakeRequest().withSession(SessionKeys.lastRequestTimestamp -> justValidTime))
      session(result) shouldBe Session(Map(SessionKeys.lastRequestTimestamp -> now().getMillis.toString, SessionKeys.userId -> "Tim"))
      status(result) shouldBe 200
    }

    "perform the wrapped action successfully and update the timestamp if the incoming timestamp is just valid" in new WithApplication(FakeApplication()) {
      val result = TestController.testWithSessionTimeoutValidation(FakeRequest().withSession(SessionKeys.lastRequestTimestamp -> justValidTime))
      session(result) shouldBe Session(Map(SessionKeys.lastRequestTimestamp -> now().getMillis.toString, SessionKeys.userId -> "Tim"))
      status(result) shouldBe 200
    }
    "perform the wrapped action successfully and update the timestamp if the incoming timestamp is valid" in new WithApplication(FakeApplication()) {
      val result = TestController.testWithSessionTimeoutValidation(FakeRequest().withSession(SessionKeys.lastRequestTimestamp -> validTime))
      session(result) shouldBe Session(Map(SessionKeys.lastRequestTimestamp -> now().getMillis.toString, SessionKeys.userId -> "Tim"))
      status(result) shouldBe 200
    }
  }

}
