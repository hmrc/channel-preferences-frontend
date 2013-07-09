package controllers.domain

import test.BaseSpec
import java.net.URI
import org.json4s.MappingException
import org.joda.time.LocalDate
import microservice.paye.domain.{ Car, Benefit }

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

  "Serialization" should {
    "transform a LocalDate to string like yyyy-MM-dd" in {

      val dateCont = LocalDateContainer(new LocalDate(2013, 3, 12))
      toRequestBody(dateCont) mustBe """{"date":"2013-03-12"}"""

    }

    "transform a benefit to json" in {
      val benefit = Benefit(31, 2013, 3333, 1, List(Car(None, None, Some(new LocalDate(2013, 3, 12)), 0, 4, 2, 2, "B", 9999)), Map.empty)
      toRequestBody(benefit) mustBe """{"benefitType":31,"taxYear":2013,"grossAmount":3333,"employmentSequenceNumber":1,"cars":[{"dateCarRegistered":"2013-03-12","employeeCapitalContribution":0,"fuelType":4,"co2Emissions":2,"engineSize":2,"mileageBand":"B","carValue":9999}],"actions":{}}"""
    }

  }

}

sealed case class VariousFields(id: String, number: Int, strings: List[String])

sealed case class HasURIField(id: URI)

case class LocalDateContainer(date: LocalDate)