package controllers.common.actions

import play.api.mvc.{Cookie, Action, Controller}
import org.mockito.{Matchers, ArgumentCaptor}
import org.mockito.Mockito._
import org.mockito.Matchers.any
import uk.gov.hmrc.common.microservice.audit.{AuditConnector, AuditEvent}
import play.api.test._
import org.slf4j.MDC
import controllers.common.{CookieEncryption, CookieNames, HeaderNames}
import uk.gov.hmrc.common.BaseSpec
import org.scalatest.concurrent.ScalaFutures
import org.bson.types.ObjectId
import uk.gov.hmrc.domain.Nino
import org.scalatest.{Inspectors, Inside}
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import uk.gov.hmrc.common.microservice.auth.domain.Pid
import uk.gov.hmrc.common.microservice.auth.domain.GovernmentGatewayCredentialResponse
import scala.Some
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.common.microservice.auth.domain.IdaCredentialResponse
import uk.gov.hmrc.common.microservice.auth.domain.Regimes
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import play.api.test.FakeApplication
import org.scalatest.mock.MockitoSugar
import scala.collection.JavaConverters._
import java.util.UUID
import controllers.common.SessionTimeoutWrapper._
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import uk.gov.hmrc.common.microservice.auth.domain.Pid
import uk.gov.hmrc.common.microservice.audit.AuditEvent
import uk.gov.hmrc.common.microservice.auth.domain.GovernmentGatewayCredentialResponse
import scala.Some
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.common.microservice.auth.domain.IdaCredentialResponse
import uk.gov.hmrc.common.microservice.auth.domain.Regimes
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import play.api.test.FakeApplication
import play.api.mvc.Cookie
import uk.gov.hmrc.utils.DateTimeUtils._
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import uk.gov.hmrc.common.microservice.auth.domain.Pid
import uk.gov.hmrc.common.microservice.audit.AuditEvent
import uk.gov.hmrc.common.microservice.auth.domain.GovernmentGatewayCredentialResponse
import scala.Some
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.common.microservice.auth.domain.IdaCredentialResponse
import uk.gov.hmrc.common.microservice.auth.domain.Regimes
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import play.api.test.FakeApplication
import play.api.mvc.Cookie
import org.apache.commons.codec.binary.Base64

class AuditTestController(override val auditConnector: AuditConnector) extends Controller with AuditActionWrapper {

  def test(userOption: Option[User]) = userOption match {
    case Some(user) => WithRequestAuditing(user) {
      user: User =>
        Action {
          request =>
            Ok("")
        }
    }
    case None => WithRequestAuditing {
      Action {
        request =>
          Ok("")
      }
    }
  }

  def failingAction(user: User) =
    WithRequestAuditing(user) {
      user: User =>
        Action {
          request =>
            throw new IllegalArgumentException("whoopsie")
      }
    }
}

class AuditActionWrapperSpec extends BaseSpec with HeaderNames with ScalaFutures with Inside with Inspectors with CookieEncryption {

  "AuditActionWrapper with traceRequestsEnabled " should {
    "generate audit events with no user details when no user is supplied" in new TestCase(traceRequests = true) {

      val response = controller.test(None)(FakeRequest("GET", "/foo").withSession(
        "sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"),
        lastRequestTimestampKey -> now.getMillis.toString,
        "userId" -> encrypt("/auth/oid/123123123"))
        .withHeaders((forwardedFor, "192.168.1.1"), (xRequestId, "govuk-tax-"))
      )

      whenReady(response) {
        result =>
          verify(auditConnector, times(2)).audit(auditEventCaptor.capture())(Matchers.any())

          val auditEvents = auditEventCaptor.getAllValues
          auditEvents.size should be(2)

          inside(auditEvents.get(0)) {
            case AuditEvent(auditSource, auditType, tags, detail, generatedAt) =>
              auditSource should be("frontend")
              auditType should be("Request")
              tags should contain(authorisation -> "Bearer /auth/oid/123123123")
              tags should contain(forwardedFor -> "192.168.1.1")
              tags should contain("path" -> "/foo")
              tags(xRequestId) contains "govuk-tax-"
              tags should not contain key("authId")
              tags should not contain key("saUtr")
              tags should not contain key("nino")
              tags should not contain key("vatNo")
              tags should not contain key("governmentGatewayId")
              tags should not contain key("idaPid")

              detail should contain("method" -> "GET")
              detail should contain("url" -> "/foo")
              detail should contain("ipAddress" -> "192.168.1.1")
              detail should contain("referrer" -> "-")
              detail should contain("userAgentString" -> "-")
          }

          inside(auditEvents.get(1)) {
            case AuditEvent(auditSource, auditType, tags, detail, generatedAt) =>
              auditSource should be("frontend")
              auditType should be("Response")
              tags should contain(authorisation -> "Bearer /auth/oid/123123123")
              tags should contain(forwardedFor -> "192.168.1.1")
              tags should contain("statusCode" -> "200")
              tags(xRequestId) contains "govuk-tax-"

              detail should contain("method" -> "GET")
              detail should contain("url" -> "/foo")
              detail should contain("ipAddress" -> "192.168.1.1")
              detail should contain("referrer" -> "-")
              detail should contain("userAgentString" -> "-")

          }
      }
    }

    "generate audit events with form data when POSTing a form" in new TestCase(traceRequests = true) {

      val response = controller.test(None)(FakeRequest("POST", "/foo").withFormUrlEncodedBody(
        "key1" -> "value1",
        "key2" -> "value2",
        "key3" -> null,
        "key4" -> ""))

      whenReady(response) {
        result =>
          verify(auditConnector, times(2)).audit(auditEventCaptor.capture())(Matchers.any())

          val auditEvents = auditEventCaptor.getAllValues
          auditEvents.size should be(2)

          inside(auditEvents.get(0)) {
            case AuditEvent(auditSource, auditType, tags, detail, generatedAt) =>
              detail should contain("formData" -> "[key1: {value1}, key2: {value2}, key3: <no values>, key4: <no values>]")
          }
      }
    }

    "generate audit events with the device finger print when it is supplied in a request cookie" in new TestCase(traceRequests = true) {
      val encryptedFingerprint = "eyJ1c2VyQWdlbnQiOiJNb3ppbGxhLzUuMCAoTWFjaW50b3NoOyBJbnRlbCBNYWMgT1MgWCAxMF84XzUpIEFwcGxlV2ViS2l0LzUzNy4zNiAoS0hUTUwsIGx" +
        "pa2UgR2Vja28pIENocm9tZS8zMS4wLjE2NTAuNDggU2FmYXJpLzUzNy4zNiIsImxhbmd1YWdlIjoiZW4tVVMiLCJjb2xvckRlcHRoIjoyNCwicmVzb2x1dGlvbiI6IjgwMHgxMj" +
        "gwIiwidGltZXpvbmUiOjAsInNlc3Npb25TdG9yYWdlIjp0cnVlLCJsb2NhbFN0b3JhZ2UiOnRydWUsImluZGV4ZWREQiI6dHJ1ZSwicGxhdGZvcm0iOiJNYWNJbnRlbCIsImRvT" +
        "m90VHJhY2siOnRydWUsIm51bWJlck9mUGx1Z2lucyI6NSwicGx1Z2lucyI6WyJTaG9ja3dhdmUgRmxhc2giLCJDaHJvbWUgUmVtb3RlIERlc2t0b3AgVmlld2VyIiwiTmF0aXZl" +
        "IENsaWVudCIsIkNocm9tZSBQREYgVmlld2VyIiwiUXVpY2tUaW1lIFBsdWctaW4gNy43LjEiXX0="

      val response = controller.test(Some(user))(FakeRequest("GET", "/foo").withCookies(Cookie(CookieNames.deviceFingerprint, encryptedFingerprint)))

      whenReady(response) {
        result =>
          verify(auditConnector, times(2)).audit(auditEventCaptor.capture())(Matchers.any())

          forAll (auditEventCaptor.getAllValues.asScala) { event: AuditEvent =>
            event.detail should contain("deviceFingerprint" -> (
              """{"userAgent":"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.48 Safari/537.36",""" +
              """"language":"en-US","colorDepth":24,"resolution":"800x1280","timezone":0,"sessionStorage":true,"localStorage":true,"indexedDB":true,"platform":"MacIntel",""" +
              """"doNotTrack":true,"numberOfPlugins":5,"plugins":["Shockwave Flash","Chrome Remote Desktop Viewer","Native Client","Chrome PDF Viewer","QuickTime Plug-in 7.7.1"]}""")
            )
            result.header.headers should not contain key ("Set-Cookie")
          }
      }
    }

    "generate audit events without the device finger print when it is not supplied in a request cookie" in new TestCase(traceRequests = true) {
      val response = controller.test(Some(user))(FakeRequest("GET", "/foo"))

      whenReady(response) {
        result =>
          verify(auditConnector, times(2)).audit(auditEventCaptor.capture())(Matchers.any())

          forAll (auditEventCaptor.getAllValues.asScala) { event: AuditEvent =>
            event.detail should not contain key ("deviceFingerprint")
            result.header.headers should not contain key ("Set-Cookie")
          }
      }
    }

    "generate audit events without the device finger print when the value supplied in the request cookie is invalid" in new TestCase(traceRequests = true) {
      val response = controller.test(Some(user))(FakeRequest("GET", "/foo").withCookies(Cookie(CookieNames.deviceFingerprint, "THIS IS SOME JUST THAT SHOULDN'T BE DECRYPTABLE *!@&£$)B__!@£$")))

      whenReady(response) {
        result =>
          verify(auditConnector, times(2)).audit(auditEventCaptor.capture())(Matchers.any())

          forAll (auditEventCaptor.getAllValues.asScala) { event: AuditEvent =>
            event.detail should not contain key ("deviceFingerprint")
            result.header.headers should contain key ("Set-Cookie")
            result.header.headers("Set-Cookie") should include regex (s"${CookieNames.deviceFingerprint}=;")
          }
      }
    }

    "generate audit events with user details when a user is supplied" in new TestCase(traceRequests = true) {

      val sessionId = s"session-${UUID.randomUUID().toString}"

      val response = controller.test(Some(user))(FakeRequest("GET", "/foo").withSession(
        SessionKeys.sessionIdName -> encrypt(sessionId),
        lastRequestTimestampKey -> now.getMillis.toString,
        "userId" -> encrypt("/auth/oid/123123123"))
        .withHeaders((forwardedFor, "192.168.1.1"), (xRequestId, "govuk-tax-")))

      whenReady(response) {
        result =>
          verify(auditConnector, times(2)).audit(auditEventCaptor.capture())(Matchers.any())

          val auditEvents = auditEventCaptor.getAllValues
          auditEvents.size should be(2)
          inside(auditEvents.get(0)) {
            case AuditEvent(auditSource, auditType, tags, detail, generatedAt) =>
              auditSource should be("frontend")
              auditType should be("Request")
              tags should contain(authorisation -> "Bearer /auth/oid/123123123")
              tags should contain(forwardedFor -> "192.168.1.1")
              tags should contain("path" -> "/foo")
              tags(xRequestId) contains "govuk-tax-"
              tags should contain(xSessionId -> sessionId)
              tags should contain("authId" -> "exAuthId")
              tags should contain("saUtr" -> "exampleUtr")
              tags should contain("nino" -> "AB123456C")
              tags should contain("vatNo" -> "123")
              tags should contain("governmentGatewayId" -> "ggCred")
              tags should contain("idaPid" -> "[idCred]")


              detail should contain("method" -> "GET")
              detail should contain("url" -> "/foo")
              detail should contain("ipAddress" -> "192.168.1.1")
              detail should contain("referrer" -> "-")
              detail should contain("userAgentString" -> "-")
          }

          inside(auditEvents.get(1)) {
            case AuditEvent(auditSource, auditType, tags, detail, generatedAt) =>
              auditSource should be("frontend")
              auditType should be("Response")
              tags should contain(authorisation -> "Bearer /auth/oid/123123123")
              tags should contain(forwardedFor -> "192.168.1.1")
              tags should contain("statusCode" -> "200")
              tags(xRequestId) contains "govuk-tax-"
              tags should contain(xSessionId -> sessionId)
              tags should contain("authId" -> "exAuthId")
              tags should contain("saUtr" -> "exampleUtr")
              tags should contain("nino" -> "AB123456C")
              tags should contain("vatNo" -> "123")
              tags should contain("governmentGatewayId" -> "ggCred")
              tags should contain("idaPid" -> "[idCred]")

              detail should contain("method" -> "GET")
              detail should contain("url" -> "/foo")
              detail should contain("ipAddress" -> "192.168.1.1")
              detail should contain("referrer" -> "-")
              detail should contain("userAgentString" -> "-")
          }

      }
    }

  }

  "AuditActionWrapper with traceRequests disabled " should {
    "not audit any events" in new TestCase(traceRequests = false) {
      MDC.put(authorisation, "/auth/oid/123123123")
      MDC.put(forwardedFor, "192.168.1.1")

      controller.test(None)(FakeRequest())
      verify(auditConnector, never).audit(any(classOf[AuditEvent]))(Matchers.any())
    }
  }

  after {
    MDC.clear
  }

}

class TestCase(traceRequests: Boolean)
  extends WithApplication(FakeApplication(additionalConfiguration = Map("govuk-tax.Test.services.datastream.traceRequests" -> traceRequests))) with MockitoSugar {
  val auditConnector: AuditConnector = mock[AuditConnector]
  val controller = new AuditTestController(auditConnector)

  val auditEventCaptor = ArgumentCaptor.forClass(classOf[AuditEvent])

  val exampleRequestId = ObjectId.get().toString
  val exampleSessionId = ObjectId.get().toString



  val userAuth = UserAuthority("exAuthId", Regimes(),
    saUtr = Some(SaUtr("exampleUtr")),
    nino = Some(Nino("AB123456C")),
    ctUtr = Some(CtUtr("asdfa")),
    vrn = Some(Vrn("123")),
    governmentGatewayCredential = Some(GovernmentGatewayCredentialResponse("ggCred")),
    idaCredential = Some(IdaCredentialResponse(List(Pid("idCred")))))
  val user = User("exUid", userAuth, RegimeRoots(), None, None)
  when(auditConnector.enabled).thenReturn(true)

}
