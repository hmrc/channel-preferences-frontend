package uk.gov.hmrc.common.microservice.vat.domain

import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.common.microservice.auth.domain.Regimes
import java.net.URI
import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.VatRegime
import controllers.common.FrontEndRedirect


class VatRegimeSpec extends BaseSpec {

  "The Is Authorised function" should {
    "return true when the user has a VAT enrolment" in {
      val regimesWithVatEnrolment = Regimes(paye = Some(new URI("/dummyPayeRegimeRoot")), vat=Some(new URI("/dummyVatRegimeRoot")))
      VatRegime.isAuthorised(regimesWithVatEnrolment) shouldBe true
    }

    "return false when the user does not have a VAT enrolment" in {
      val regimesWithoutVatEnrolment = Regimes(paye = Some(new URI("/dummyPayeRegimeRoot")), ct=Some(new URI("/dummyCtRegimeRoot")))
      VatRegime.isAuthorised(regimesWithoutVatEnrolment) shouldBe false
    }

  }

  "The Unauthorised Landing Page" should {
    "point to the business tax home page" in {
      VatRegime.unauthorisedLandingPage shouldBe FrontEndRedirect.businessTaxHome
    }
  }
}

