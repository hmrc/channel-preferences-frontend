package uk.gov.hmrc.common.microservice.domain

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.auth.domain.Authority
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import org.mockito.Mockito._

class UserSpec extends BaseSpec with MockitoSugar {

  "a Government Gateway User" should {

    "have their display name the same as name form GG" in {

      val regimes = RegimeRoots()

      val user = User("id", mock[Authority], regimes, Some("John Small"), None)

      user.displayName shouldBe Some("John Small")

    }

  }

  "a PAYE User" should {

    "have their display name the same as the name from PayeRoot" in {
      val payeRoot = PayeRoot("nino", "Mr", "Steve", None, "Rogers", "John Densmore", "05-07-1990", Map.empty, Map.empty, Map.empty)

      val regimes = RegimeRoots(paye = Some(payeRoot))

      val user = User("id", mock[Authority], regimes, None, None)

      user.displayName shouldBe Some("Steve")
    }

  }

}
