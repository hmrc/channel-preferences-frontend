package controllers.common.actions

import play.api.mvc.{Cookie, Action, Controller}
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.mockito.Matchers.any
import uk.gov.hmrc.common.microservice.audit.{AuditConnector, AuditEvent}
import play.api.test._
import org.slf4j.MDC
import controllers.common.{CookieNames, HeaderNames}
import uk.gov.hmrc.common.BaseSpec
import org.scalatest.concurrent.ScalaFutures
import org.bson.types.ObjectId
import uk.gov.hmrc.domain.Nino
import org.scalatest.Inside
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

class AuditActionWrapperSpec extends BaseSpec with HeaderNames with ScalaFutures with Inside {

  "AuditActionWrapper with traceRequestsEnabled " should {
    "generate audit events with no user details when no user is supplied" in new TestCase(traceRequests = true) {

      MDC.put(authorisation, "/auth/oid/123123123")
      MDC.put(forwardedFor, "192.168.1.1")
      MDC.put(requestId, exampleRequestId)

      val response = controller.test(None)(FakeRequest("GET", "/foo"))

      whenReady(response) {
        result =>
          verify(auditConnector, times(2)).audit(auditEventCaptor.capture())

          val auditEvents = auditEventCaptor.getAllValues
          auditEvents.size should be(2)

          inside(auditEvents.get(0)) {
            case AuditEvent(auditSource, auditType, tags, detail, generatedAt) =>
              auditSource should be("frontend")
              auditType should be("Request")
              tags should contain(authorisation -> "/auth/oid/123123123")
              tags should contain(forwardedFor -> "192.168.1.1")
              tags should contain("path" -> "/foo")
              tags should contain(requestId -> exampleRequestId)
              tags should not contain key(xSessionId)
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
              tags should contain(authorisation -> "/auth/oid/123123123")
              tags should contain(forwardedFor -> "192.168.1.1")
              tags should contain("statusCode" -> "200")
              tags should contain(requestId -> exampleRequestId)
              tags should not contain key(xSessionId)


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
          verify(auditConnector, times(2)).audit(auditEventCaptor.capture())

          val auditEvents = auditEventCaptor.getAllValues
          auditEvents.size should be(2)

          inside(auditEvents.get(0)) {
            case AuditEvent(auditSource, auditType, tags, detail, generatedAt) =>
              detail should contain("formData" -> "[key1: {value1}, key2: {value2}, key3: <no values>, key4: <no values>]")
          }
      }
    }

    "generate audit events with the device finger print when it is supplied in a request cookie" in new TestCase(traceRequests = true) {
      val fingerprint = "QuickTime%20Plug-in%207.7.1%3A%3AThe%20QuickTime%20Plugin%20allows%20you%20to%20view%20a%20wide%20variety%20of%20multimedia%20content%20in%20web%20pages." +
        "%20For%20more%20information%2C%20visit%20the%20%3CA%20HREF%3Dhttp%3A%2F%2Fwww.apple.com%2Fquicktime%3EQuickTime%3C%2FA%3E%20Web%20site.%3A%3Avideo%2Fx-msvideo~avi%2Cvfw%2Caudio%2" +
        "Fmp3~mp3%2Cswa%2Caudio%2Fmpeg3~mp3%2Cswa%2Cvideo%2F3gpp2~3g2%2C3gp2%2Caudio%2Fx-caf~caf%2Caudio%2Fmpeg~mpeg%2Cmpg%2Cm1s%2Cm1a%2Cmp2%2Cmpm%2Cmpa%2Cm2a%2Cmp3%2Cswa%2Cvideo%2Fquicktime~" +
        "mov%2Cqt%2Cmqv%2Caudio%2Fx-mpeg3~mp3%2Cswa%2Cvideo%2Fmp4~mp4%2Capplication%2Fx-sdp~sdp%2Caudio%2Fwav~wav%2Cbwf%2Cvideo%2Favi~avi%2Cvfw%2Caudio%2Fx-ac3~ac3%2Caudio%2Fmp4~mp4%2Cvideo%2Fx-" +
        "m4v~m4v%2Capplication%2Fsdp~sdp%2Caudio%2Fx-wav~wav%2Cbwf%2Caudio%2Fx-aiff~aiff%2Caif%2Caifc%2Ccdda%2Cvideo%2Fx-mpeg~mpeg%2Cmpg%2Cm1s%2Cm1v%2Cm1a%2Cm75%2Cm15%2Cmp2%2Cmpm%2Cmpv%2Cmpa%2Cvid" +
        "eo%2F3gpp~3gp%2C3gpp%2Cvideo%2Fmsvideo~avi%2Cvfw%2Caudio%2Fx-mpeg~mpeg%2Cmpg%2Cm1s%2Cm1a%2Cmp2%2Cmpm%2Cmpa%2Cm2a%2Cmp3%2Cswa%2Caudio%2Fvnd.qcelp~qcp%2Cqcp%2Caudio%2Fx-mp3~mp3%2Cswa%2Capplic" +
        "ation%2Fx-rtsp~rtsp%2Crts%2Caudio%2FAMR~AMR%2Cvideo%2Fsd-video~sdv%2Caudio%2Faiff~aiff%2Caif%2Caifc%2Ccdda%2Cvideo%2Fmpeg~mpeg%2Cmpg%2Cm1s%2Cm1v%2Cm1a%2Cm75%2Cm15%2Cmp2%2Cmpm%2Cmpv%2Cmpa%2Cau" +
        "dio%2F3gpp2~3g2%2C3gp2%2Caudio%2Faac~aac%2Cadts%2Caudio%2Fac3~ac3%2Caudio%2Fx-m4b~m4b%2Caudio%2Fx-m4p~m4p%2Caudio%2Fx-gsm~gsm%2Capplication%2Fx-mpeg~amc%2Caudio%2Fx-aac~aac%2Cadts%2Caudio%2Fbasi" +
        "c~au%2Csnd%2Culw%2Caudio%2Fx-m4a~m4a%2Caudio%2F3gpp~3gp%2C3gpp%3BJava%20Applet%20Plug-in%3A%3ADisplays%20Java%20applet%20content%2C%20or%20a%20placeholder%20if%20Java%20is%20not%20installed.%3A%3A" +
        "application%2Fx-java-applet%3Bjavafx%3D2.2.25~%2Capplication%2Fx-java-applet%3Bversion%3D1.4~%2Capplication%2Fx-java-applet%3Bversion%3D1.2.1~%2Capplication%2Fx-java-applet%3Bversion%3D1.2.2~%2Cappl" +
        "ication%2Fx-java-applet%3Bversion%3D1.3~%2Capplication%2Fx-java-vm~%2Capplication%2Fx-java-applet%3Bversion%3D1.1.1~%2Capplication%2Fx-java-vm-npruntime~%2Capplication%2Fx-java-applet%3Bversion%3D" +
        "1.2~%2Capplication%2Fx-java-applet%3Bversion%3D1.7~%2Capplication%2Fx-java-applet%3Bjpi-version%3D1.7.0_25~%2Capplication%2Fx-java-applet%3Bversion%3D1.4.1~%2Capplication%2Fx-java-applet~javaapp" +
        "let%2Capplication%2Fx-java-applet%3Bversion%3D1.1.2~%2Capplication%2Fx-java-applet%3Bversion%3D1.1~%2Capplication%2Fx-java-applet%3Bversion%3D1.1.3~%2Capplication%2Fx-java-applet%3Bversion%3D1.6~%" +
        "2Capplication%2Fx-java-applet%3Bversion%3D1.4.2~%2Capplication%2Fx-java-applet%3Bversion%3D1.3.1~%2Capplication%2Fx-java-applet%3Bversion%3D1.5~%2Capplication%2Fx-java-applet%3Bdeploy%3D10.25.2~"


      val response = controller.test(Some(user))(FakeRequest("GET", "/foo").withCookies(Cookie(CookieNames.deviceFingerprint, fingerprint)))

      whenReady(response) {
        result =>
          verify(auditConnector, times(2)).audit(auditEventCaptor.capture())

          val auditEvents = auditEventCaptor.getAllValues
          auditEvents.size should be(2)
          auditEvents.get(0).detail should contain("deviceFingerprint" -> (
                "QuickTime Plug-in 7.7.1::The QuickTime Plugin allows you to view a wide variety of multimedia content i" +
                "n web pages. For more information, visit the <A HREF=http://www.apple.com/quicktime>QuickTime</A> Web s" +
                "ite.::video/x-msvideo~avi,vfw,audio/mp3~mp3,swa,audio/mpeg3~mp3,swa,video/3gpp2~3g2,3gp2,audio/x-caf~ca" +
                "f,audio/mpeg~mpeg,mpg,m1s,m1a,mp2,mpm,mpa,m2a,mp3,swa,video/quicktime~mov,qt,mqv,audio/x-mpeg3~mp3,swa," +
                "video/mp4~mp4,application/x-sdp~sdp,audio/wav~wav,bwf,video/avi~avi,vfw,audio/x-ac3~ac3,audio/mp4~mp4,v" +
                "ideo/x-m4v~m4v,application/sdp~sdp,audio/x-wav~wav,bwf,audio/x-aiff~aiff,aif,aifc,cdda,video/x-mpeg~mpe" +
                "g,mpg,m1s,m1v,m1a,m75,m15,mp2,mpm,mpv,mpa,video/3gpp~3gp,3gpp,video/msvideo~avi,vfw,audio/x-mpeg~mpeg,m" +
                "pg,m1s,m1a,mp2,mpm,mpa,m2a,mp3,swa,audio/vnd.qcelp~qcp,qcp,audio/x-mp3~mp3,swa,application/x-rtsp~rtsp," +
                "rts,audio/AMR~AMR,video/sd-video~sdv,audio/aiff~aiff,aif,aifc,cdda,video/mpeg~mpeg,mpg,m1s,m1v,m1a,m75," +
                "m15,mp2,mpm,mpv,mpa,audio/3gpp2~3g2,3gp2,audio/aac~aac,adts,audio/ac3~ac3,audio/x-m4b~m4b,audio/x-m4p~m" +
                "4p,audio/x-gsm~gsm,application/x-mpeg~amc,audio/x-aac~aac,adts,audio/basic~au,snd,ulw,audio/x-m4a~m4a,a" +
                "udio/3gpp~3gp,3gpp;Java Applet Plug-in::Displays Java applet content, or a placeholder if Java is not i" +
                "nstalled.::application/x-java-applet;javafx=2.2.25~,application/x-java-applet;version=1.4~,application/" +
                "x-java-applet;version=1.2.1~,application/x-java-applet;version=1.2.2~,application/x-java-applet;version" +
                "=1.3~,application/x-java-vm~,application/x-java-applet;version=1.1.1~,application/x-java-vm-npruntime~," +
                "application/x-java-applet;version=1.2~,application/x-java-applet;version=1.7~,application/x-java-applet" +
                ";jpi-version=1.7.0_25~,application/x-java-applet;version=1.4.1~,application/x-java-applet~javaapplet,ap" +
                "plication/x-java-applet;version=1.1.2~,application/x-java-applet;version=1.1~,application/x-java-applet" +
                ";version=1.1.3~,application/x-java-applet;version=1.6~,application/x-java-applet;version=1.4.2~,applica" +
                "tion/x-java-applet;version=1.3.1~,application/x-java-applet;version=1.5~,application/x-java-applet;depl" +
                "oy=10.25.2~"))
      }
    }

    "generate audit events with user details when a user is supplied" in new TestCase(traceRequests = true) {
      MDC.put(authorisation, "/auth/oid/123123123")
      MDC.put(forwardedFor, "192.168.1.1")
      MDC.put(requestId, exampleRequestId)
      MDC.put(xSessionId, exampleSessionId)

      val response = controller.test(Some(user))(FakeRequest("GET", "/foo"))

      whenReady(response) {
        result =>
          verify(auditConnector, times(2)).audit(auditEventCaptor.capture())

          val auditEvents = auditEventCaptor.getAllValues
          auditEvents.size should be(2)
          inside(auditEvents.get(0)) {
            case AuditEvent(auditSource, auditType, tags, detail, generatedAt) =>
              auditSource should be("frontend")
              auditType should be("Request")
              tags should contain(authorisation -> "/auth/oid/123123123")
              tags should contain(forwardedFor -> "192.168.1.1")
              tags should contain("path" -> "/foo")
              tags should contain(requestId -> exampleRequestId)
              tags should contain(xSessionId -> exampleSessionId)
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
              tags should contain(authorisation -> "/auth/oid/123123123")
              tags should contain(forwardedFor -> "192.168.1.1")
              tags should contain("statusCode" -> "200")
              tags should contain(requestId -> exampleRequestId)
              tags should contain(xSessionId -> exampleSessionId)
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
      verify(auditConnector, never).audit(any(classOf[AuditEvent]))
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
