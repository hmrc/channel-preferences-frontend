package endToEnd.scala.uk.gov.hmrc.personal.resource

import java.net.URI


class SaToAuthAndNpsConnectivityISpec{ // {

//  val auth = new AuthMicroServiceIntegration(8500)
//  val npsStub = new NpsStubServiceIntegration(8080)
//
//
//  override protected def beforeAll() {
//    npsStub.start(fail("Hod Stub ended immediately after starting."))
//
//    super.beforeAll()
//  }
//
//  override protected def afterAll() {
//    npsStub.stop()
//
//    super.afterAll()
//  }
//
//
//  "starting all of the external services that personal requires" should {
//    "allow requests to be made" in {
//
//      val response = extractJson[PersonalTaxData](httpGET.GET(resource("/personal/paye/AA020513B")), 200).serialized
//      response mustBe PersonalTaxData(
//
//        nino = "AA020513B",
//        firstName = "John",
//        surname = "Densmore",
//        name = "John Densmore",
//
//        links = Map(
//          "benefits" -> new URI("/personal/paye/AA020513B/benefits/2013"),
//          "taxCode" -> new URI("/personal/paye/AA020513B/tax-codes/2013"),
//          "employments" -> new URI("/personal/paye/AA020513B/employments/2013")
//        )
//      )
//    }
//
//  }
}
