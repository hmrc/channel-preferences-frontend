package controllers.bt.spechelpers

import uk.gov.hmrc.domain._
import java.net.URI
import org.joda.time.DateTime
import uk.gov.hmrc.common.microservice.sa.domain.SaDomain.SaRoot
import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.VatRoot
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeDomain.EpayeRoot
import uk.gov.hmrc.common.microservice.ct.domain.CtDomain.CtRoot
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import org.mockito.Mockito._
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeDomain.EpayeLinks
import uk.gov.hmrc.common.microservice.sa.domain.SaDomain.SaJsonRoot
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.common.microservice.auth.domain.Regimes
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.domain.SaUtr
import scala.util.Success
import uk.gov.hmrc.common.microservice.ct.domain.CtDomain.CtJsonRoot
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeDomain.EpayeJsonRoot
import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.VatJsonRoot
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot

trait GeoffFisherExpectations {

  self: WithBusinessTaxApplication =>

  def geoffFisherAuthId = "/auth/oid/geoff"
  def geoffFisherSaUtr = SaUtr("/sa/individual/123456789012")
  def geoffFisherCtUtr = CtUtr("/ct/6666644444")
  def geoffFisherEmpRef = EmpRef("342", "sdfdsf")
  def geoffFisherVrn = Vrn("456345576")

  override val userId: Option[String] = Some(geoffFisherAuthId)

  override val affinityGroup: Option[String] = Some("someAffinityGroup")

  override val nameFromGovernmentGateway: Option[String] = Some("Geoffrey From Government Gateway")

  override val governmentGatewayToken: Option[String] = Some("someToken")

  def geoffFisherAuthority = UserAuthority(
    id = geoffFisherAuthId,
    regimes = Regimes(
      sa = Some(URI.create(geoffFisherSaUtr.toString)),
      vat = Some(URI.create(geoffFisherVrn.toString)),
      epaye = Some(URI.create(geoffFisherEmpRef.toString)),
      ct = Some(URI.create(geoffFisherCtUtr.toString))),
    previouslyLoggedInAt = Some(new DateTime(1000L)),
    saUtr = Some(geoffFisherSaUtr),
    vrn = Some(geoffFisherVrn),
    ctUtr = Some(geoffFisherCtUtr),
    empRef = Some(geoffFisherEmpRef))

  def geoffFisherSaJsonRoot = SaJsonRoot(links = Map("something" -> s"$geoffFisherSaUtr/stuff"))
  def geoffFisherSaRoot = SaRoot(geoffFisherSaUtr, geoffFisherSaJsonRoot)

  def geoffFisherVatJsonRoot = VatJsonRoot(links = Map("something" -> s"$geoffFisherVrn/stuff"))
  def geoffFisherVatRoot = VatRoot(vrn = geoffFisherVrn, links = Map("something" -> s"$geoffFisherVrn/stuff"))

  def geoffFisherEpayeJsonRoot = EpayeJsonRoot(EpayeLinks(accountSummary = Some(s"$geoffFisherEmpRef/blah") ) )
  def geoffFisherEpayeRoot = EpayeRoot(geoffFisherEmpRef, geoffFisherEpayeJsonRoot)

  def geoffFisherCtJsonRoot = CtJsonRoot(Map("something" -> s"$geoffFisherCtUtr/dsffds"))
  def geoffFisherCtRoot = CtRoot(geoffFisherCtUtr, geoffFisherCtJsonRoot)



  implicit val geoffFisherUser = User(
    userId = geoffFisherAuthId,
    userAuthority = geoffFisherAuthority,
    regimes = RegimeRoots(paye = None, sa = Some(Success(geoffFisherSaRoot)), vat = Some(Success(geoffFisherVatRoot)),
      epaye = Some(Success(geoffFisherEpayeRoot)), ct = Some(Success(geoffFisherCtRoot))),
    nameFromGovernmentGateway = nameFromGovernmentGateway,
    decryptedToken = governmentGatewayToken
  )

  when(mockAuthMicroService.authority(geoffFisherAuthId)).thenReturn(Some(geoffFisherAuthority))
  when(mockSaConnector.root(geoffFisherSaUtr.toString)).thenReturn(geoffFisherSaJsonRoot)
  when(mockVatConnector.root(geoffFisherVrn.toString)).thenReturn(geoffFisherVatJsonRoot)
  when(mockEpayeConnector.root(geoffFisherEmpRef.toString)).thenReturn(geoffFisherEpayeJsonRoot)
  when(mockCtConnector.root(geoffFisherCtUtr.toString)).thenReturn(geoffFisherCtJsonRoot)
}


trait NonBusinessUserExpectations {

  self: WithBusinessTaxApplication =>

  val johnDensmoreAuthId = "/auth/oid/densmore"
  val johnDensmoreNino = Nino("AB654433C")

  override val userId: Option[String] = Some(johnDensmoreAuthId)

  val johnDensmoreAuthority = UserAuthority(
    id = johnDensmoreAuthId,
    regimes = Regimes(paye = Some(URI.create(s"/paye/$johnDensmoreNino"))),
    nino = Some(johnDensmoreNino))

  val johnDensmorePayeRoot = mock[PayeRoot]

  val johnDensmoreUser = User(
    userId = johnDensmoreAuthId,
    userAuthority = johnDensmoreAuthority,
    regimes = RegimeRoots(paye = Some(Success(johnDensmorePayeRoot)), sa = None, vat = None, epaye = None, ct = None),
    nameFromGovernmentGateway = None,
    decryptedToken = None
  )

  when(mockAuthMicroService.authority(johnDensmoreAuthId)).thenReturn(Some(johnDensmoreAuthority))
  when(mockPayeMicroService.root(johnDensmoreNino.toString)).thenReturn(johnDensmorePayeRoot)
}