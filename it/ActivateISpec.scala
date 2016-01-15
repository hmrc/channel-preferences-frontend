import org.scalatest.mock.MockitoSugar
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.WS
import uk.gov.hmrc.crypto.ApplicationCrypto.QueryParameterCrypto
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.domain.TaxIdentifier

class ActivateISpec extends PreferencesFrontEndServer with EmailSupport with MockitoSugar {

  "activate" should {
    "return neither BAD_REQUEST nor 5xx if activating sa-all" in new TestCase {
      val saUtr = Generate.utr
      val response = `/paperless/activate/:form-type/:tax-identifier`("sa-all", saUtr)().put().futureValue
      response.status should (not be BAD_REQUEST and not be NOT_FOUND and be < 500)
    }

    "return NOT_FOUND if no preferences for a given utr and nino if activating notice-of-coding" in new TestCase {
      val nino = Generate.nino
      val saUtr = Generate.utr
      val response = `/paperless/activate/:form-type/:tax-identifier`("notice-of-coding", nino)(saUtr).put().futureValue
      response.status should be (NOT_FOUND)
    }
  }

  trait TestCase extends TestCaseWithFrontEndAuthentication {
    import play.api.Play.current

    val returnUrl = "/test/return/url"
    val returnLinkText = "Continue"

    def `/paperless/activate/:form-type/:tax-identifier`(formType: String, taxIdentifier: TaxIdentifier)(additionalUserTaxIdentifiers: TaxIdentifier*) = new {
      private val url = WS.url(resource(s"/paperless/activate/$formType/${taxIdentifier.value}"))
        .withHeaders(createGGAuthorisationHeader(taxIdentifier +: additionalUserTaxIdentifiers: _*), cookieForTaxIdentifiers(taxIdentifier +: additionalUserTaxIdentifiers: _*))
        .withQueryString(
          "returnUrl" -> QueryParameterCrypto.encrypt(PlainText(returnUrl)).value,
          "returnLinkText" -> QueryParameterCrypto.encrypt(PlainText(returnLinkText)).value
        )

      private val formTypeBody = Json.parse("""{"active":true}""")

      def put() = url.put(formTypeBody)
    }
  }
}
