package views

import uk.gov.hmrc.common.BaseSpec
import views.html.saml_auth_form
import play.api.test.{FakeApplication, WithApplication, FakeRequest}

class SamlAuthFormTemplateSpec extends BaseSpec with PageSugar {

  "SamlAuthForm" should {
    "have the right url and data" in new WithApplication(FakeApplication()) {
      val url = "http://www.foo.com/post"
      val formData = "Some data"

      val page = saml_auth_form(url, formData)(FakeRequest())

      page("form").first.attr("action") should be(url)
      page("form input[type=hidden]").first.`val` should be(formData)
    }
  }

}
