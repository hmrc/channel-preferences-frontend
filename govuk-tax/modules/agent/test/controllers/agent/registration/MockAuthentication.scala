package controllers.agent.registration

import uk.gov.hmrc.microservice.auth.AuthMicroService
import uk.gov.hmrc.microservice.paye.PayeMicroService
import org.mockito.Mockito._
import uk.gov.hmrc.microservice.domain.User
import scala.Some
import uk.gov.hmrc.microservice.domain.RegimeRoots
import uk.gov.hmrc.microservice.paye.domain.PayeRoot
import org.mockito.Matchers
import uk.gov.hmrc.microservice.auth.domain.{ Regimes, UserAuthority }
import java.net.URI
import org.scalatest.mock.MockitoSugar

trait MockAuthentication extends MockitoSugar {

  var mockAuthMicroService = mock[AuthMicroService]
  var mockPayeMicroService = mock[PayeMicroService]

  val id = "wshakespeare"
  val authority = s"/auth/oid/$id"
  val uri = "/personal/paye/blah"

  val payeRoot = PayeRoot("CE927349E", 1, "Mr", "Will", None, "Shakespeare", "Will Shakespeare", "1983-01-02", Map(), Map())
  val user = User(id, null, RegimeRoots(Some(payeRoot), None, None), None, None)

  when(mockPayeMicroService.root(uri)).thenReturn(payeRoot)
  when(mockAuthMicroService.authority(Matchers.anyString())).thenReturn(Some(UserAuthority(authority, Regimes(paye = Some(URI.create(uri))))))
}
