package views.helpers

import uk.gov.hmrc.common.BaseSpec
import play.api.data.Form
import play.api.data.Forms._
import views.html.helpers.inputRadioGroup
import org.jsoup.Jsoup
import play.api.test.Helpers._
import org.jsoup.nodes.Element
import play.api.test.FakeRequest

class InputRadioGroupSpec extends BaseSpec {

  case class DummyFormData(radioValue: String)

  def dummyForm = Form(
    mapping(
      "radioValue" -> text(maxLength = 10)

    )(DummyFormData.apply)(DummyFormData.unapply))

  "@helpers.inputRadioGroup" should {
    "render an option" in {
      val doc = Jsoup.parse(contentAsString(inputRadioGroup(dummyForm("radioValue"), Seq("myValue" -> "myLabel"),'_inputClass -> "myInputClass")))
      val input = doc.getElementById("radioValue-myvalue")

      input.attr("type") shouldBe "radio"
      input.attr("name") shouldBe "radioValue"
      input.attr("value") shouldBe "myValue"
      input.attr("class") shouldBe "myInputClass"
      input.parent().text() shouldBe "myLabel"
    }

    "render label for radio button with the correct class" in {
      val doc = Jsoup.parse(contentAsString(inputRadioGroup(dummyForm("radioValue"), Seq("myValue" -> "myLabel"),'_innerLabelClass -> "myInnerLabelClass")))
      doc.getElementsByAttributeValue("for","radioValue-myvalue").attr("class") shouldBe "myInnerLabelClass"
    }

    "render multiple options" in {
      val doc = Jsoup.parse(contentAsString(inputRadioGroup(dummyForm("radioValue"), Seq("myValue1" -> "myLabel1","myValue2" -> "myLabel2"))))
      doc.getElementById("radioValue-myvalue1") should not be null
      doc.getElementById("radioValue-myvalue2") should not be null
    }

    "render a selected option" in {
      val doc = Jsoup.parse(contentAsString(inputRadioGroup(dummyForm.fill(DummyFormData("myValue"))("radioValue"), Seq("myValue" -> "myLabel"))))
      val input = doc.getElementById("radioValue-myvalue")
      input.attr("checked") shouldBe "checked"
    }

    "render the radio group label"  in {
      val doc = Jsoup.parse(contentAsString(inputRadioGroup(dummyForm("radioValue"), Seq("myValue" -> "myLabel"),
        '_label -> "My Radio Group",
        '_labelClass -> "myLabelClass",
        '_divClass -> "myDivClass"
      )))

      val firstLabel: Element = doc.getElementsByTag("label").first()
      firstLabel.ownText() shouldBe "My Radio Group"
      firstLabel.attr("class") should include("myLabelClass")
      firstLabel.attr("class") should not include "form-field--error"
      firstLabel.attr("class") should include("myDivClass")
    }

    "renders errors" in {
      val field = dummyForm.bindFromRequest()(FakeRequest().withFormUrlEncodedBody("radioValue" -> "Value is too long!")).fold(
        error => {
          error("radioValue")
        },
        data => throw new Exception
      )
      val doc = Jsoup.parse(contentAsString(inputRadioGroup(field, Seq("myValue" -> "myLabel"),'_inputClass -> "myInputClass")))
      doc.getElementsByTag("label").first().attr("class") should include("form-field--error")
      doc.getElementsByClass("error-notification").first().text() shouldBe "error.maxLength"
    }
  }

}
