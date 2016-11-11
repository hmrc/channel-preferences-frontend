package model

import helpers.ConfigHelper
import model.FormType.formTypeBinder
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application

class FormTypeSpec extends WordSpec with Matchers with OneAppPerSuite {

  override implicit lazy val app : Application = ConfigHelper.fakeApp

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