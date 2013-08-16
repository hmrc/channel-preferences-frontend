package controllers.agent

import play.api.test.{ FakeRequest, FakeApplication, WithApplication }
import uk.gov.hmrc.common.BaseSpec
import controllers.common.service.Encryption
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.microservice.paye.PayeMicroService
import org.mockito.{Matchers, Mockito}
import uk.gov.hmrc.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.microservice.domain.{RegimeRoots, User}
import views.PageSugar
import org.jsoup.Jsoup

class AgentControllerSpec extends BaseSpec with Encryption with MockitoSugar {

  import play.api.test.Helpers._

  private def controller = new AgentController

  "The sro check page" should {
    "include two agreements" in new WithApplication(FakeApplication()) {
      val content = contentAsString(controller.sroCheck()(FakeRequest()))
      content should include("Yes, I am the Senior Responsible Officer")
      content should include("I accept the Terms &amp; Conditions")
    }

    "Return a BadRequest error if agreements are not checked" in new WithApplication(FakeApplication()) {

      val result = controller.submitAgreement()(newRequest("false", "false"))
      status(result) shouldBe 400
      contentAsString(result) should include("Please specify that you are the Senior Responsible Officer")
      contentAsString(result) should include("Please accept the terms and conditions")

    }

    "Return a Redirect if agreements are checked" in new WithApplication(FakeApplication()) {

      val result = controller.submitAgreement()(newRequest("true", "true"))

      status(result) shouldBe 303
      header("Location", result) shouldBe Some("/samllogin")

    }
  }

  "The submit agreement page" should {
    "add a register agent entry in the session" in new WithApplication(FakeApplication()) {
      val result = controller.submitAgreement()(newRequest("true", "true"))

      session(result).data("register agent") should equal("true")
    }
  }

  "The contact details page" should {
    "display known agent info" in new WithApplication(FakeApplication()) {

      val payeRoot = PayeRoot("CE927349E", 1, "Mr", "Will", None, "Shakespeare", "Will Shakespeare", "1983-01-02", Map(), Map())
      val user = User("wshakespeare", null, RegimeRoots(Some(payeRoot), None, None), None, None)

      val result = new AgentController().contactDetailsFunction(user, null)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#title").first().`val` should be ("Mr")
      doc.select("#firstName").first().`val` should be ("Will")
      doc.select("#middleName").first().`val` should be ("")
      doc.select("#lastName").first().`val` should be ("Shakespeare")
      doc.select("#nino").first().`val` should be ("CE927349E")
      doc.select("#dateOfBirth").first().`val` should be ("1983-01-02")
    }
  }

  def newRequest(sro: String, tnc: String) =
    FakeRequest().withFormUrlEncodedBody("sroAgreement" -> sro, "tncAgreement" -> tnc)


  override val encryptionKey = "eU1qMlpESFRPN0hRNGJxNg=="
}
