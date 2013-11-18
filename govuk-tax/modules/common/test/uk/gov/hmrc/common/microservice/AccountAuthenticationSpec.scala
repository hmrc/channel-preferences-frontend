package uk.gov.hmrc.common.microservice

import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import controllers.common.{GovernmentGateway, Ida}
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeRegime
import uk.gov.hmrc.common.microservice.ct.domain.CtRegime
import uk.gov.hmrc.common.microservice.vat.domain.VatRegime
import uk.gov.hmrc.common.microservice.sa.domain.SaRegime

class AccountAuthenticationSpec extends BaseSpec {

  "The authentication type for each account type should be correctly defined" should {

    "The paye account should be authenticated by IDA" in {
      PayeRegime.authenticationType should be(Ida)
    }

    "The epaye account should be authenticated by Government Gateway" in {
      EpayeRegime.authenticationType should be(GovernmentGateway)
    }

    "The CT account should be authenticated by Government Gateway" in {
      CtRegime.authenticationType should be(GovernmentGateway)
    }

    "The VAT account should be authenticated by Government Gateway" in {
      VatRegime.authenticationType should be(GovernmentGateway)
    }

    "The SA account should be authenticated by Government Gateway" in {
      SaRegime.authenticationType should be(GovernmentGateway)
    }

  }

}
