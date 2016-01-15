package model

import model.FormType.formTypeBinder
import org.scalatest.{Matchers, WordSpec}
import uk.gov.hmrc.play.test.WithFakeApplication

class FormTypeSpec extends WordSpec with Matchers with WithFakeApplication {

  "Bind known form types" should {
      for {
        (formType, formTypeName) <- Seq(SaAll -> SaAll.value, NoticeOfCoding -> NoticeOfCoding.value)
      }
        s"work for $formTypeName to $formType" in {
          formTypeBinder.bind("", formTypeName) should be(Right(formType))
        }
    }

  "Binding of an unknown form-type should be not successful" in {
    formTypeBinder.bind("", "unrecognized") should be(Left("unrecognized is not a valid form-type"))
  }

  "Unbinding a formType object" should {
    "return formType's value" in {
      formTypeBinder.unbind("", SaAll) should be (SaAll.value)
    }
  }
}