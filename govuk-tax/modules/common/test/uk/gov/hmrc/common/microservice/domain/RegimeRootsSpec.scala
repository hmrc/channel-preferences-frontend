package uk.gov.hmrc.common.microservice.domain

import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.domain.{SaUtr, CtUtr, Vrn, EmpRef}
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import uk.gov.hmrc.common.microservice.ct.domain.CtRoot
import uk.gov.hmrc.common.microservice.vat.domain.VatRoot
import uk.gov.hmrc.common.microservice.epaye.domain.{EpayeLinks, EpayeRoot}

class RegimeRootsSpec extends BaseSpec {
  def empRef: EmpRef = EmpRef("123", "456")

  def ctUtr: CtUtr = CtUtr("aCtUtr")

  def someRoot(sa: Option[SaRoot] = Some(SaRoot(SaUtr("someUtr"), Map("link1" -> "http://sa/1")))) = RegimeRoots(
    sa = sa,
    ct = Some(CtRoot(ctUtr, Map("link1" -> "http://ct/1"))),
    vat = Some(VatRoot(Vrn("someVrn"), Map("link1" -> "http://vat/1"))),
    epaye = Some(EpayeRoot(empRef, EpayeLinks(Some("http://epaye/1")))),
    paye = None)

  "Matching RegimeRoot" should {
    "successfully match based on the unapply method" in {
      val regimeRoots = RegimeRoots(vat = Some(VatRoot(Vrn("someVrn"), Map("link1" -> "http://vatLink"))))
      regimeRoots match {
        case RegimeRoots(paye, sa, vat, epaye, ct) => {
          paye shouldBe None
          epaye shouldBe None
          sa shouldBe None
          vat shouldBe Some(VatRoot(Vrn("someVrn"), Map("link1" -> "http://vatLink")))
          ct shouldBe None

        }
        case _ => fail("Failed to match RegimeRoot")
      }
    }
  }


  "Equals upon RegimeRoots" should {
    "return true where the properties are equal but the objects are not the same" in {
      val aRoot = someRoot()
      val anotherRoot = someRoot()

      aRoot should not be theSameInstanceAs(anotherRoot)
      aRoot shouldBe anotherRoot
    }
    "return false when at least one property is not equal" in {
      val aRoot = someRoot()
      val anotherRoot = someRoot(None)

      aRoot should not be theSameInstanceAs(anotherRoot)
      aRoot should not be anotherRoot
    }

    "HashCode upon RegimeRoots" should {
      "always return the same value when two instances are equal" in {
        someRoot().hashCode shouldBe someRoot().hashCode
        someRoot(None).hashCode shouldBe someRoot(None).hashCode
      }
    }
  }
}