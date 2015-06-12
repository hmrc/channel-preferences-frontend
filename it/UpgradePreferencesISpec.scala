import java.io.ByteArrayOutputStream


import com.ning.http.client.FluentCaseInsensitiveStringsMap
import com.ning.http.client.providers.netty.NettyResponse
import play.api.http.{ContentTypeOf, Writeable}
import play.api.libs.json.Json
import play.api.libs.ws.{WSResponse, WS}
import play.api.mvc.Results.EmptyContent
import play.api.test.FakeRequest
import play.core.parsers.FormUrlEncodedParser
import com.ning.http.multipart.{StringPart, FilePart, MultipartRequestEntity, Part}
;

class UpgradePreferencesISpec extends PreferencesFrontEndServer with EmailSupport {

  "Upgrading preferences should" should {
    "set upgraded terms and conditions and allow subsequent activation"  in new UpgradeTestCase  {
      createOptedInVerifiedPreferenceWithNino()

      `/preferences/paye/individual/:nino/activations`(nino,authHeader).post().futureValue.status should be (412)

      val response = `/upgrade-email-reminders`.post(accept = true).futureValue
//      response.status should be (404)
      response.underlying[NettyResponse].getUri.getPath should be(returnUrl)

      `/preferences/paye/individual/:nino/activations`(nino,authHeader).post().futureValue.status should be (200)
    }
  }

  trait UpgradeTestCase extends TestCaseWithFrontEndAuthentication {
    import play.api.Play.current

    val returnUrl = "/test/return/url"
    override def utr: String = "1097172564"

    override val gatewayId: String = "UpgradePreferencesISpec"
    val authHeader = bearerTokenHeader()

    val nino = "CE123457D"

    val parts = Array[Part](new StringPart("submitButton", "accepted"))
    val mpre = new MultipartRequestEntity(parts, new FluentCaseInsensitiveStringsMap)
    val baos = new ByteArrayOutputStream
    mpre.writeRequest(baos)
    val bytes = baos.toByteArray
    val contentType = mpre.getContentType

    val `/upgrade-email-reminders` = new {
      def post(accept: Boolean) = WS.url(resource("/account/account-details/sa/upgrade-email-reminders")).withHeaders(cookie).withQueryString(("returnUrl" -> returnUrl)).
//        post(bytes)(Writeable.wBytes, ContentTypeOf(Some(contentType)))
        post(Map("submitButton" -> Seq("accepted")))
      //post(EmptyContent())
    }

    def createOptedInVerifiedPreferenceWithNino() : WSResponse = {
      await(`/preferences-admin/sa/individual`.delete(utr))
      `/portal/preferences/sa/individual`.postPendingEmail(utr, uniqueEmail) should (have(status(200)) or have(status(201)))
      `/preferences-admin/sa/individual`.verifyEmailFor(utr)
      await(`/preferences-admin/sa/process-nino-determination`.post())
    }
  }
}
