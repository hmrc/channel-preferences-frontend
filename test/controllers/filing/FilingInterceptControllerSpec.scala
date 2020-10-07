/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers.filing

import java.net.URLEncoder.{ encode => urlEncode }

import connectors.EntityResolverConnector
import model.Encrypted
import org.joda.time.{ DateTime, DateTimeZone }
import org.mockito.Matchers.{ eq => meq, _ }
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{ BeforeAndAfterEach, Matchers, OptionValues, WordSpec }
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{ AnyContent, Request }
import play.api.test.FakeRequest
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class FilingInterceptControllerSpec
    extends WordSpec with Matchers with MockitoSugar with BeforeAndAfterEach with ScalaFutures with OptionValues
    with GuiceOneAppPerSuite {

  import play.api.test.Helpers._

  val mockEntityResolverConnector = mock[EntityResolverConnector]
  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "sso.encryption.key"          -> "P5xsJ9Nt+quxGZzB4DeLfw==",
        "sso.encryption.previousKeys" -> Seq.empty
      )
      .overrides(bind[EntityResolverConnector].toInstance(mockEntityResolverConnector))
      .build()

  val crypto = app.injector.instanceOf[TokenEncryption]
  val controller = app.injector.instanceOf[FilingInterceptController]

  override def beforeEach() = reset(mockEntityResolverConnector)

  "Preferences pages" should {
    "redirect to the portal when no preference exists for a specific utr" in new TestCase {
      when(mockEntityResolverConnector.getEmailAddress(meq(validUtr))).thenReturn(Future.successful(None))

      val page = controller.redirectWithEmailAddress(validToken, encodedReturnUrl, None)(FakeRequest())
      status(page) shouldBe 303
      header("Location", page).value should be(decodedReturnUrl)
      verify(mockEntityResolverConnector, times(1)).getEmailAddress(meq(validUtr))
    }

    "redirect to the portal when a preference for email already exists for a specific utr" in new TestCase {
      when(mockEntityResolverConnector.getEmailAddress(meq(validUtr))).thenReturn(Future.successful(Some(emailAddress)))

      val page = controller.redirectWithEmailAddress(validToken, encodedReturnUrl, None)(FakeRequest())
      status(page) shouldBe 303
      private val value = header("Location", page).value
      value should be(decodedReturnUrlWithEmailAddress)
      verify(mockEntityResolverConnector, times(1)).getEmailAddress(meq(validUtr))
    }

    "redirect to the portal when a preference for paper already exists for a specific utr" in new TestCase {
      when(mockEntityResolverConnector.getEmailAddress(meq(validUtr))).thenReturn(Future.successful(None))

      val page = controller.redirectWithEmailAddress(validToken, encodedReturnUrl, None)(FakeRequest())
      status(page) shouldBe 303
      header("Location", page).value should be(decodedReturnUrl)
      verify(mockEntityResolverConnector, times(1)).getEmailAddress(meq(validUtr))
    }

    "redirect to the portal when preferences already exist for a specific utr and an email address was passed to the platform" in new TestCase {
      when(mockEntityResolverConnector.getEmailAddress(meq(validUtr))).thenReturn(Future.successful(Some(emailAddress)))

      val page = controller.redirectWithEmailAddress(
        validToken,
        encodedReturnUrl,
        Some(Encrypted(EmailAddress("other@me.com"))))(FakeRequest())
      status(page) shouldBe 303
      header("Location", page).value should be(decodedReturnUrlWithEmailAddress)
    }

    "redirect to portal if the token is expired on the landing page" in new TestCase {
      val page = controller.redirectWithEmailAddress(expiredToken, encodedReturnUrl, None)(FakeRequest())

      status(page) shouldBe 303
      header("Location", page).get should equal(decodedReturnUrl)
    }

    "redirect to portal if the token is not valid on the landing page" in new TestCase {
      val page = controller.redirectWithEmailAddress(incorrectToken, encodedReturnUrl, None)(FakeRequest())

      status(page) shouldBe 303
      header("Location", page).get should equal(decodedReturnUrl)
    }

    "return bad request if redirect_url is not in the allowlist" in new TestCase {

      val page = controller.redirectWithEmailAddress(validToken, encodedUrlNotOnAllowlist, None)(FakeRequest())
      status(page) shouldBe 400
    }
  }

  trait TestCase {
    //   val crypto = CryptoWithKeysFromConfig(baseConfigKey = "sso.encryption")
    val emailAddress = "foo@bar.com"
    val validUtr = SaUtr("1234567")
    lazy val validToken =
      urlEncode(crypto.encrypt(PlainText(s"$validUtr:${DateTime.now(DateTimeZone.UTC).getMillis}")).value, "UTF-8")
    lazy val expiredToken = urlEncode(
      crypto.encrypt(PlainText(s"$validUtr:${DateTime.now(DateTimeZone.UTC).minusDays(1).getMillis}")).value,
      "UTF-8")
    lazy val incorrectToken = "this is an incorrect token khdskjfhasduiy3784y37yriuuiyr3i7rurkfdsfhjkdskh"
    val decodedReturnUrl = "http://localhost:8080/portal?exampleQuery=exampleValue"
    val encodedReturnUrl = urlEncode(decodedReturnUrl, "UTF-8")
    lazy val decodedReturnUrlWithEmailAddress =
      s"$decodedReturnUrl&email=${urlEncode(crypto.encrypt(PlainText(emailAddress)).value, "UTF-8")}"
    val encodedUrlNotOnAllowlist = urlEncode("http://notOnAllowlist/something", "UTF-8")

    val request = FakeRequest()

    implicit def hc: HeaderCarrier = any()

    def request(
      optIn: Option[Boolean],
      mainEmail: Option[String] = None,
      mainEmailConfirmation: Option[String] = None): Request[AnyContent] = {

      val params = (
        Seq(mainEmail.map { v =>
          "email.main" -> v
        })
          ++ Seq(mainEmailConfirmation.map { v =>
            ("email.confirm", v)
          })
          ++ Seq(optIn.map { v =>
            ("opt-in", v.toString)
          })
      ).flatten

      FakeRequest().withFormUrlEncodedBody(params: _*)

    }

  }

}
