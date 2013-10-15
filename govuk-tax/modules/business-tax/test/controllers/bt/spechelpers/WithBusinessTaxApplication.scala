package controllers.bt.spechelpers

import play.api.test.{FakeRequest, FakeApplication, WithApplication}
import controllers.bt.{BusinessTaxController, MicroServiceMocks}
import org.scalatest.mock.MockitoSugar
import controllers.common.CookieEncryption
import org.joda.time.{Duration, DateTimeZone, DateTime}
import controllers.bt.regimeViews.AccountSummariesFactory
import play.api.mvc.Request
import play.api.templates.Html
import play.api.Logger
import java.util.UUID
import controllers.common.SessionTimeoutWrapper._
import uk.gov.hmrc.common.microservice.domain.User
import scala.Some
import controllers.bt.regimeViews.AccountSummaries

abstract class WithBusinessTaxApplication
  extends WithApplication(FakeApplication())
  with MicroServiceMocks
  with MockitoSugar
  with CookieEncryption {

  val currentTime = new DateTime(2013, 9, 27, 15, 7, 22, 232, DateTimeZone.UTC)
  val lastRequestTimestamp = currentTime.minus(Duration.standardMinutes(1))
  val mockAccountSummariesFactory = mock[AccountSummariesFactory]
  val userId: Option[String] = None
  val affinityGroup: Option[String] = None
  val nameFromGovernmentGateway: Option[String] = None
  val governmentGatewayToken: Option[String] = None

  trait Expectations {

    def makeAPaymentLandingPage(user: User): String

    def businessTaxHomepage(user: User, portalHref: String, accountSummaries: AccountSummaries): String

    def buildPortalUrl(user: User, request: Request[AnyRef], base: String): String
  }

  val expectations = mock[Expectations]

  val businessTaxController = new BusinessTaxController(mockAccountSummariesFactory) with MockedMicroServices {

    override def now: () => DateTime = () => currentTime

    override private[bt] def makeAPaymentLandingPage()(implicit user: User): Html = {
      Logger.debug("RENDERING makeAPaymentLandingPage")
      Html(expectations.makeAPaymentLandingPage(user))
    }

    override private[bt] def businessTaxHomepage(portalHref: String, accountSummaries: AccountSummaries)(implicit user: User): Html = {
      Logger.debug("RENDERING businessTaxHomePage")
      Html(expectations.businessTaxHomepage(user, portalHref, accountSummaries))
    }

    override def buildPortalUrl(base: String)(implicit request: Request[AnyRef], user: User): String = {
      expectations.buildPortalUrl(user, request, base)
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

    val cleanSession = session.collect { case (paramName, Some(paramValue)) => (paramName, paramValue)}

    FakeRequest().withSession(cleanSession:_*)
  }
}