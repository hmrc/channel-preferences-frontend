package controllers.bt.spechelpers

import play.api.test.{FakeRequest, FakeApplication, WithApplication}
import org.scalatest.mock.MockitoSugar
import controllers.common.CookieEncryption
import org.joda.time.{Duration, DateTimeZone, DateTime}
import play.api.templates.Html
import play.api.Logger
import java.util.UUID
import controllers.common.SessionTimeoutWrapper._
import scala.Some
import controllers.bt.{VatController, MicroServiceMocks}

abstract class WithVatApplication extends WithApplication(FakeApplication()) with MicroServiceMocks with MockitoSugar with CookieEncryption {

  val currentTime = new DateTime(2013, 9, 27, 15, 7, 22, 232, DateTimeZone.UTC)

  val lastRequestTimestamp: DateTime = currentTime.minus(Duration.standardMinutes(1))

  val userId: Option[String] = None

  val affinityGroup: Option[String] = None

  val nameFromGovernmentGateway: Option[String] = None

  val governmentGatewayToken: Option[String] = None

  trait Expectations {

    def makeAPayment: String

  }

  val expectations = mock[Expectations]

  val vatController = new VatController with MockedMicroServices {
    override def now: () => DateTime = () => currentTime

    override private[bt] def makeAPaymentPage: Html = {
      Logger.debug("RENDERING makeAPayment")
      Html(expectations.makeAPayment)
    }
  }

  implicit lazy val request = {

    val session: Seq[(String, Option[String])] = Seq(
      "sessionId" -> Some(encrypt(s"session-${UUID.randomUUID().toString}")),
      lastRequestTimestampKey -> Some(lastRequestTimestamp.getMillis.toString),
      "userId" -> userId.map(encrypt),
      "name" -> nameFromGovernmentGateway.map(encrypt),
      "token" -> governmentGatewayToken.map(encrypt),
      "affinityGroup" -> affinityGroup.map(encrypt))

    val cleanSession = session.collect {
      case (paramName, Some(paramValue)) => (paramName, paramValue)
    }

    FakeRequest().withSession(cleanSession: _*)
  }
}
