package controllers.domain

import java.net.URI
import org.json4s.MappingException
import org.joda.time.LocalDate
import uk.gov.hmrc.common.microservice.paye.domain.{Questionable, Car, Benefit}
import uk.gov.hmrc.common.BaseSpec

class TransformSpec extends BaseSpec {

  import controllers.common.domain.Transform._

  implicit def stringToURI(s: String) = URI.create(s)

  "Json" should {
    "be transformed into associated object" in {
      fromResponse[VariousFields]("""{"id":"123456789","number": 87,"strings" : ["one", "two","three"]}""") should be(VariousFields("123456789", 87, List("one", "two", "three")))
    }

    "be transformed into the object type even if the json has more fields than the object" in {
      fromResponse[HasURIField]("""{"id":"123456789","number": 87,"strings" : ["one", "two","three"]}""") should be(HasURIField("123456789"))
    }

    "fail transformation when the json has less fields than the object type" in {
      evaluating {
        fromResponse[VariousFields]("""{"id":"/has/uri/123456789"}""")
      } should produce[MappingException]
    }

    "with URI are deserializer" in {
      fromResponse[HasURIField]("""{"id":"/has/uri/123456789"}""") should be(HasURIField("/has/uri/123456789"))
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
      toRequestBody(dateCont) shouldBe """{"date":"2013-03-12"}"""

    }

    "transform a benefit to json" in {
      val benefit = Benefit(31, 2013, 3333, 1, None, None, None, None, None, None, None, Some(Car(None, None, Questionable(new LocalDate(2013, 3, 12), true), Some(0), Some("diesel"), Some(2), Some(1400), None, Some(9999), None, None)), Map.empty, Map.empty)
      toRequestBody(benefit) shouldBe """{
                                        |  "benefitType": 31,
                                        |  "taxYear": 2013,
                                        |  "grossAmount": 3333,
                                        |  "employmentSequenceNumber": 1,
                                        |  "car": {
                                        |    "dateCarRegistered": {
                                        |      "value": "2013-03-12",
                                        |      "displayable": true
                                        |    },
                                        |    "employeeCapitalContribution": 0,
                                        |    "fuelType": "diesel",
                                        |    "co2Emissions": 2,
                                        |    "engineSize": 1400,
                                        |    "carValue": 9999
                                        |  },
                                        |  "actions": {},
                                        |  "calculations": {}
                                        |}""".stripMargin.replaceAll("\\s","")
    }

  }

}

sealed case class VariousFields(id: String, number: Int, strings: List[String])

sealed case class HasURIField(id: URI)

case class LocalDateContainer(date: LocalDate)