package controllers.common.actions

import play.api.mvc.{ Action, Controller }
import uk.gov.hmrc.common.microservice.MockMicroServicesForTests
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.mockito.Matchers.any
import uk.gov.hmrc.common.microservice.audit.AuditEvent
import play.api.test._
import org.slf4j.MDC
import controllers.common.HeaderNames
import uk.gov.hmrc.common.BaseSpec
import org.scalatest.concurrent.ScalaFutures
import org.bson.types.ObjectId
import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, User}
import uk.gov.hmrc.common.microservice.auth.domain._
import uk.gov.hmrc.domain.{Vrn, CtUtr, Nino, SaUtr}
import org.scalatest.Inside
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
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

class AuditTestController extends Controller with AuditActionWrapper with MockMicroServicesForTests {

  def test(userOption: Option[User]) = userOption match {
    case Some(user) => WithRequestAuditing(user) { user: User  =>
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

}

class AuditActionWrapperSpec extends BaseSpec with HeaderNames with ScalaFutures with Inside {

  "AuditActionWrapper with traceRequestsEnabled " should {
    "generate audit events with user details when a user is supplied" in new WithApplication(
      FakeApplication(additionalConfiguration = Map("govuk-tax.Test.services.datastream.traceRequests" -> true))) {

      val auditEventCaptor = ArgumentCaptor.forClass(classOf[AuditEvent])
      val exampleRequestId = ObjectId.get().toString

      MDC.put(authorisation, "/auth/oid/123123123")
      MDC.put(forwardedFor, "192.168.1.1")
      MDC.put(requestId, exampleRequestId)


      val controller = new AuditTestController()

      when(controller.auditMicroService.enabled).thenReturn(true)

      val response = controller.test(None)(FakeRequest("GET", "/foo"))

      whenReady(response) { result =>
        verify(controller.auditMicroService, times(2)).audit(auditEventCaptor.capture())

        val auditEvents = auditEventCaptor.getAllValues
        auditEvents.size should be(2)

        inside(auditEvents.get(0)) { case AuditEvent(auditSource, auditType, tags, detail) =>
          auditSource should be("frontend")
          auditType should be("Request")
          tags should contain(authorisation -> "/auth/oid/123123123")
          tags should contain(forwardedFor -> "192.168.1.1")
          tags should contain("path" -> "/foo")
          tags should contain(requestId -> exampleRequestId)
          tags should not contain key (xSessionId)
          tags should not contain key ("authId")
          tags should not contain key ("saUtr")
          tags should not contain key ("nino")
          tags should not contain key ("vatNo")
          tags should not contain key ("governmentGatewayId")
          tags should not contain key ("idaPid")

          detail should contain("method" -> "GET")
          detail should contain("url" -> "/foo")
          detail should contain("ipAddress" -> "192.168.1.1")
          detail should contain("referrer" -> "-")
          detail should contain("userAgentString" -> "-")
        }

        inside(auditEvents.get(1)) { case AuditEvent(auditSource, auditType, tags, detail) =>
          auditSource should be("frontend")
          auditType should be("Response")
          tags should contain(authorisation -> "/auth/oid/123123123")
          tags should contain(forwardedFor -> "192.168.1.1")
          tags should contain("statusCode" -> "200")
          tags should contain(requestId -> exampleRequestId)
          tags should not contain key (xSessionId)


          detail should contain("method" -> "GET")
          detail should contain("url" -> "/foo")
          detail should contain("ipAddress" -> "192.168.1.1")
          detail should contain("referrer" -> "-")
          detail should contain("userAgentString" -> "-")

        }
      }
    }

    "generate audit events with form data when POSTing a form" in new WithApplication(
      FakeApplication(additionalConfiguration = Map("govuk-tax.Test.services.datastream.traceRequests" -> true))) {

      val auditEventCaptor = ArgumentCaptor.forClass(classOf[AuditEvent])
      val exampleRequestId = ObjectId.get().toString

      MDC.put(authorisation, "/auth/oid/123123123")
      MDC.put(forwardedFor, "192.168.1.1")
      MDC.put(requestId, exampleRequestId)

      val controller = new AuditTestController()

      when(controller.auditMicroService.enabled).thenReturn(true)

      val response = controller.test(None)(FakeRequest("POST", "/foo").withFormUrlEncodedBody(
        "key1" -> "value1",
        "key2" -> "value2",
        "key3" -> null,
        "key4" -> ""))

      whenReady(response) { result =>
        verify(controller.auditMicroService, times(2)).audit(auditEventCaptor.capture())

        val auditEvents = auditEventCaptor.getAllValues
        auditEvents.size should be(2)

        inside(auditEvents.get(0)) { case AuditEvent(auditSource, auditType, tags, detail) =>
          detail should contain("formData" -> "[key1: {value1}, key2: {value2}, key3: <no values>, key4: <no values>]")
        }
      }
    }

    "generate audit events with no user details when a user is not supplied" in new WithApplication(
      FakeApplication(additionalConfiguration = Map("govuk-tax.Test.services.datastream.traceRequests" -> true))) {

      val userAuth = UserAuthority("exAuthId", Regimes(),
        saUtr = Some(SaUtr("exampleUtr")),
        nino = Some(Nino("AB123456C")),
        ctUtr = Some(CtUtr("asdfa")),
        vrn = Some(Vrn("123")),
        governmentGatewayCredential = Some(GovernmentGatewayCredentialResponse("ggCred")),
        idaCredential = Some(IdaCredentialResponse(List(Pid("idCred")))))
      val user = User("exUid", userAuth, RegimeRoots(), None, None)

        val auditEventCaptor = ArgumentCaptor.forClass(classOf[AuditEvent])
        val exampleRequestId = ObjectId.get().toString
        val exampleSessionId = ObjectId.get().toString

        MDC.put(authorisation, "/auth/oid/123123123")
        MDC.put(forwardedFor, "192.168.1.1")
        MDC.put(requestId, exampleRequestId)
        MDC.put(xSessionId, exampleSessionId)

        val controller = new AuditTestController()

        when(controller.auditMicroService.enabled).thenReturn(true)

        val response = controller.test(Some(user))(FakeRequest("GET", "/foo"))

        whenReady(response) { result =>
          verify(controller.auditMicroService, times(2)).audit(auditEventCaptor.capture())

          val auditEvents = auditEventCaptor.getAllValues
          auditEvents.size should be(2)
          inside(auditEvents.get(0)) { case AuditEvent(auditSource, auditType, tags, detail) =>
            auditSource should be("frontend")
            auditType should be("Request")
            tags should contain(authorisation -> "/auth/oid/123123123")
            tags should contain(forwardedFor -> "192.168.1.1")
            tags should contain("path" -> "/foo")
            tags should contain(requestId -> exampleRequestId)
            tags should contain(xSessionId-> exampleSessionId)
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

          inside(auditEvents.get(1)) { case AuditEvent(auditSource, auditType, tags, detail) =>
            auditSource should be("frontend")
            auditType should be("Response")
            tags should contain(authorisation -> "/auth/oid/123123123")
            tags should contain(forwardedFor -> "192.168.1.1")
            tags should contain("statusCode" -> "200")
            tags should contain(requestId -> exampleRequestId)
            tags should contain(xSessionId-> exampleSessionId)
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
    "not audit any events" in new WithApplication(FakeApplication()) {

      val controller = new AuditTestController()

      MDC.put(authorisation, "/auth/oid/123123123")
      MDC.put(forwardedFor, "192.168.1.1")

      controller.test(None)(FakeRequest())
      verify(controller.auditMicroService, never).audit(any(classOf[AuditEvent]))
    }
  }

  after {
    MDC.clear
  }

}
