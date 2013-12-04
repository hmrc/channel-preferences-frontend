package uk.gov.hmrc.common.microservice.vat.domain

import uk.gov.hmrc.common.BaseSpec
import controllers.common.FrontEndRedirect

class VatRegimeSpec extends BaseSpec {

  import controllers.domain.AuthorityUtils._

  "The Is Authorised function" should {
    "return true when the user has a VAT enrolment" in {
      VatRegime.isAuthorised(vatAuthority("dummy", "dymmy").accounts) shouldBe true
    }

    "return false when the user does not have a VAT enrolment" in {
      VatRegime.isAuthorised(ctAuthority("dummy", "dummy").accounts) shouldBe false
    }

  }

  "The Unauthorised Landing Page" should {
    "point to the business tax home page" in {
      VatRegime.unauthorisedLandingPage shouldBe FrontEndRedirect.businessTaxHome
    }
  }
}

