package uk.gov.hmrc.common.microservice.domain

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import org.mockito.Mockito._

class UserSpec extends BaseSpec with MockitoSugar {

  "a Government Gateway User" should {

    "have their display name the same as name form GG" in {

      val regimes = RegimeRoots()

      val user = User("id", mock[UserAuthority], regimes, Some("John Small"), None)

      user.displayName shouldBe Some("John Small")

    }

  }

  "a PAYE User" should {

    "have their display name the same as the name from PayeRoot" in {

      val payeRoot = mock[PayeRoot]

      val regimes = RegimeRoots(paye = Some(payeRoot))

      val user = User("id", mock[UserAuthority], regimes, None, None)

      when(payeRoot.name).thenReturn("John Densmore")

      user.displayName shouldBe Some("John Densmore")

    }

  }

}
