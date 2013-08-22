package views

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.matchers.ShouldMatchers
import views.html.saml_auth_form

class SamlAuthFormTemplateSpec extends BaseSpec with PageSugar {

  "SamlAuthForm" should {
    "have the right url and data" in {
      val url = "http://www.foo.com/post"
      val formData = "Some data"

      val page = saml_auth_form(url, formData)

      page("form").first.attr("action") should be(url)
      page("form input[type=hidden]").first.`val` should be(formData)
    }
  }

}
