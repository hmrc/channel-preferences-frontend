package controllers

import controllers.AuthorityUtils._
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.test.UnitSpec

import scala.util.Random

class FindTaxIdentifierSpec extends UnitSpec {

  "finding tax id from Auth" should {
    "return sa utr if only utr is present" in  new TestCase {
      val validUtr = SaUtr("1234567890")
      val authContext = AuthContext(authority = saAuthority("userId", validUtr.value), nameFromSession = Some("Ciccio"), governmentGatewayToken = None)
      findTaxIdentifier.findTaxIdentifier(authContext) should be (validUtr)
    }

    "return nino if only nino is present" in  new TestCase {
      val nino = Nino(f"CE${Random.nextInt(100000)}%06dD")
      val authContext = AuthContext(authority = payeAuthority("userId", nino.value), nameFromSession = Some("Ciccio"), governmentGatewayToken = None)
      findTaxIdentifier.findTaxIdentifier(authContext) should be (nino)
    }

    "return utr if only nino is present" in  new TestCase {
      val nino = Nino(f"CE${Random.nextInt(100000)}%06dD")
      val validUtr = SaUtr("1234567890")
      val authContext = AuthContext(authority = ninoAndPayeAuthority("userId", validUtr.value, nino.value), nameFromSession = Some("Ciccio"), governmentGatewayToken = None)
      findTaxIdentifier.findTaxIdentifier(authContext) should be (validUtr)
    }

    "return empty if neither nino or utr is present" in  new TestCase {
      val authContext = AuthContext(authority = emptyAuthority("userId"), nameFromSession = Some("Ciccio"), governmentGatewayToken = None)
      a [RuntimeException] should be thrownBy findTaxIdentifier.findTaxIdentifier(authContext)
    }
  }

  trait TestCase {
    val findTaxIdentifier = new FindTaxIdentifier {}
  }

}
