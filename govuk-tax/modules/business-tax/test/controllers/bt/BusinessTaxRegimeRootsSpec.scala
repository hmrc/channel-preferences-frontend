package controllers.bt

import controllers.common.service.ConnectorsApi
import uk.gov.hmrc.common.microservice.vat.VatConnector
import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.common.microservice.vat.domain.{VatRoot, VatJsonRoot}
import org.mockito.Mockito._
import org.mockito.Matchers.any
import scala.Some
import uk.gov.hmrc.common.microservice.MicroServiceException
import scala.concurrent.Future
import controllers.common.actions.HeaderCarrier
import controllers.domain.AuthorityUtils._
import uk.gov.hmrc.common.microservice.ct.CtConnector
import uk.gov.hmrc.common.microservice.epaye.EpayeConnector
import uk.gov.hmrc.common.microservice.sa.SaConnector
import uk.gov.hmrc.common.microservice.sa.domain.{SaRoot, SaJsonRoot}
import uk.gov.hmrc.common.microservice.ct.domain.{CtRoot, CtJsonRoot}
import uk.gov.hmrc.common.microservice.epaye.domain.{EpayeRoot, EpayeJsonRoot}
import uk.gov.hmrc.domain.{Vrn, EmpRef, SaUtr, CtUtr}
import org.scalatest.mock.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import java.net.ConnectException


class BusinessTaxRegimeRootsSpec extends BaseSpec with ScalaFutures {

  " BusinessTaxRegimeRoots" should {

    "throw the exception if VAT microservice throws something other than MicroServiceException or ConnectException" in new BusinessTaxRegimeRootsSetup {

      when(vatConnector.root(any())(any())).thenReturn(Future.failed(new Exception))

      val businessTax = new  BusinessTaxRegimeRoots {
        override val connectors = mockConnectors
      }

      val result = businessTax.regimeRoots(authority)(new HeaderCarrier())
      whenReady(result.failed) {
        failedResult =>
          failedResult shouldBe an[Exception]
      }
    }

    "return an invalid VAT Root if the VAT service is down" in new BusinessTaxRegimeRootsSetup {

      when(vatConnector.root(any())(any())).thenReturn(failedWithMicroServiceException)

      val businessTax = new  BusinessTaxRegimeRoots {
        override val connectors = mockConnectors
      }
      val roots = businessTax.regimeRoots(authority)(new HeaderCarrier())

      roots.vat shouldBe None
      roots.ct shouldBe Some(CtRoot(ctUtr, ctJsonRoot))
      roots.epaye shouldBe Some(EpayeRoot(empRef, epayeJsonRoot))
      roots.sa shouldBe Some(SaRoot(saUtr, saJsonRoot))
    }

    "return an invalid Epaye Root if the Epaye service is down" in new BusinessTaxRegimeRootsSetup {

      when(epayeConnector.root(any())(any())).thenReturn(failedWithMicroServiceException)

      val businessTax = new  BusinessTaxRegimeRoots {
        override val connectors = mockConnectors
      }
      val roots = businessTax.regimeRoots(authority)(new HeaderCarrier())

      roots.vat shouldBe Some(VatRoot(vrn, vatJsonRoot))
      roots.ct shouldBe Some(CtRoot(ctUtr, ctJsonRoot))
      roots.epaye shouldBe None
      roots.sa shouldBe Some(SaRoot(saUtr, saJsonRoot))
    }

    "return an invalid CT Root if the CT service is down" in new BusinessTaxRegimeRootsSetup {

      when(ctConnector.root(any())(any())).thenReturn(failedWithConnectException)

      val businessTax = new  BusinessTaxRegimeRoots {
        override val connectors = mockConnectors
      }
      val roots = businessTax.regimeRoots(authority)(new HeaderCarrier())

      roots.vat shouldBe Some(VatRoot(vrn, vatJsonRoot))
      roots.ct shouldBe None
      roots.epaye shouldBe Some(EpayeRoot(empRef, epayeJsonRoot))
      roots.sa shouldBe Some(SaRoot(saUtr, saJsonRoot))
    }

    "return an invalid SA Root if the SA service is down" in new BusinessTaxRegimeRootsSetup {

      when(saConnector.root(any())(any())).thenReturn(failedWithMicroServiceException)

      val businessTax = new  BusinessTaxRegimeRoots {
        override val connectors = mockConnectors
      }
      val roots = businessTax.regimeRoots(authority)(new HeaderCarrier())

      roots.vat shouldBe Some(VatRoot(vrn, vatJsonRoot))
      roots.ct shouldBe Some(CtRoot(ctUtr, ctJsonRoot))
      roots.epaye shouldBe Some(EpayeRoot(empRef, epayeJsonRoot))
      roots.sa shouldBe None
    }
  }
}

class BusinessTaxRegimeRootsSetup extends MockitoSugar {

  val failedWithMicroServiceException = Future.failed(new MicroServiceException("Simulation of microservice down", null))
  val failedWithConnectException = Future.failed(new ConnectException)

  val saUtr = SaUtr("saUtr")
  val vrn = Vrn("Vrn")
  val empRef = EmpRef("emp","Ref")
  val ctUtr = CtUtr("ctUtr")
  val authority = allBizTaxAuthority("id", saUtr.utr, ctUtr.utr, vrn.vrn, empRef.toString)

  val ctConnector = mock[CtConnector]
  val ctJsonRoot = CtJsonRoot(Map.empty)
  when(ctConnector.root(any())(any())).thenReturn(Future.successful(ctJsonRoot))

  val epayeConnector = mock[EpayeConnector]
  val epayeJsonRoot = EpayeJsonRoot(null)
  when(epayeConnector.root(any())(any())).thenReturn(Future.successful(epayeJsonRoot))

  val saConnector = mock[SaConnector]
  val saJsonRoot = SaJsonRoot(Map.empty)
  when(saConnector.root(any())(any())).thenReturn(Future.successful(saJsonRoot))

  val vatConnector = mock[VatConnector]
  val vatJsonRoot = VatJsonRoot(Map.empty)
  when(vatConnector.root(any())(any())).thenReturn(Future.successful(vatJsonRoot))

  val mockConnectors = mock[ConnectorsApi]
  when(mockConnectors.vatConnector).thenReturn(vatConnector)
  when(mockConnectors.ctConnector).thenReturn(ctConnector)
  when(mockConnectors.saConnector).thenReturn(saConnector)
  when(mockConnectors.epayeConnector).thenReturn(epayeConnector)
}

