package controllers

import test.BaseSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import play.api.test.{ FakeRequest, WithApplication }
import microservice.MockMicroServicesForTests
import microservice.auth.AuthMicroService
import org.mockito.Mockito._
import microservice.sa.domain._
import microservice.auth.domain.{ Regimes, UserAuthority }
import play.api.test.FakeApplication
import scala.Some
import play.api.mvc.{ AnyContent, Action }
import microservice.sa.SaMicroService
import org.joda.time.DateTime
import java.net.URI
import sun.security.krb5.internal.crypto.Aes128
import controllers.SessionTimeoutWrapper._
import microservice.auth.domain.UserAuthority
import microservice.sa.domain.SaRoot
import microservice.sa.domain.SaIndividualAddress
import scala.Some
import microservice.auth.domain.Regimes
import microservice.sa.domain.SaPerson
import play.api.test.FakeApplication

class SaControllerSpec extends BaseSpec with ShouldMatchers with MockitoSugar with CookieEncryption {

  import play.api.test.Helpers._

  private val mockAuthMicroService = mock[AuthMicroService]
  private val mockSaMicroService = mock[SaMicroService]

  private def controller = new SaController with MockMicroServicesForTests {
    override val authMicroService = mockAuthMicroService
    override val saMicroService = mockSaMicroService
  }

  when(mockAuthMicroService.authority("/auth/oid/gfisher")).thenReturn(
    Some(UserAuthority("someIdWeDontCareAboutHere", Regimes(paye = Some(URI.create("/personal/paye/DF334476B")), sa = Some(URI.create("/personal/sa/123456789012")), vat = Set(URI.create("/some-undecided-url"))), Some(new DateTime(1000L)))))

  when(mockSaMicroService.root("/personal/sa/123456789012")).thenReturn(
    SaRoot(
      utr = "123456789012",
      links = Map(
        "personalDetails" -> "/personal/sa/123456789012/details")
    )
  )

  val nameFromSa = "Geoff Fisher From SA"
  val nameFromGovernmentGateway = "Geoffrey From Government Gateway"

  "The details page" should {
    "show the individual SA address of Geoff Fisher" in new WithApplication(FakeApplication()) {

      when(mockSaMicroService.person("/personal/sa/123456789012/details")).thenReturn(
        Some(SaPerson(
          name = nameFromSa,
          utr = "123456789012",
          address = SaIndividualAddress(
            addressLine1 = "address line 1",
            addressLine2 = "address line 2",
            addressLine3 = "address line 3",
            addressLine4 = "address line 4",
            addressLine5 = "address line 5",
            postcode = "postcode",
            foreignCountry = "foreign country",
            additionalDeliveryInformation = "additional delivery info"
          )
        ))
      )

      val content = request(controller.details)

      content should include(nameFromSa)
      content should include(nameFromGovernmentGateway)
      content should include("address line 1")
      content should include("address line 2")
      content should include("address line 3")
      content should include("address line 4")
      content should include("address line 5")
      content should include("postcode")
    }

    "display an error page if personal details do not come back from backend service" in new WithApplication(FakeApplication()) {
      when(mockSaMicroService.person("/personal/sa/123456789012/details")).thenReturn(None)
      val result = controller.details(FakeRequest().withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt("<governmentGatewayToken/>"), sessionTimestampKey -> controller.now().getMillis.toString))
      status(result) should be(404)
    }
  }

  def request(action: Action[AnyContent]): String = {
    val result = action(FakeRequest().withSession("userId" -> encrypt("/auth/oid/gfisher"), "name" -> encrypt(nameFromGovernmentGateway), "token" -> encrypt("<governmentGatewayToken/>"), sessionTimestampKey -> controller.now().getMillis.toString))

    status(result) should be(200)

    contentAsString(result)
  }
}
