package controller.domain

import test.BaseSpec
import java.net.URI
import org.json4s.MappingException

class TransformSpec extends BaseSpec {

  import controllers.domain.Transform._

  implicit def stringToURI(s: String) = URI.create(s)

  "Json" should {
    "be transformed into associated object" in {
      fromResponse[VariousFields]("""{"id":"123456789","number": 87,"strings" : ["one", "two","three"]}""") must be(VariousFields("123456789", 87, List("one", "two", "three")))
    }

    "be transformed into the object type even if the json has more fields than the object" in {
      fromResponse[HasURIField]("""{"id":"123456789","number": 87,"strings" : ["one", "two","three"]}""") must be(HasURIField("123456789"))
    }

    "fail transformation when the json has less fields than the object type" in {
      evaluating {
        fromResponse[VariousFields]("""{"id":"/has/uri/123456789"}""")
      } should produce[MappingException]
    }

    "with URI are deserializer" in {
      fromResponse[HasURIField]("""{"id":"/has/uri/123456789"}""") must be(HasURIField("/has/uri/123456789"))
    }

    "fail transformation when null json value is provided" in {
      val caught = evaluating {
        fromResponse[VariousFields](null)
      } should produce[IllegalArgumentException]

      caught.getMessage should be("A string value is required for transformation")
    }

    "fail transformation when empty string value is provided" in {
      evaluating {
        fromResponse[VariousFields]("")
      } should produce[IllegalArgumentException]
    }
  }

}

sealed case class VariousFields(id: String, number: Int, strings: List[String])

sealed case class HasURIField(id: URI)
