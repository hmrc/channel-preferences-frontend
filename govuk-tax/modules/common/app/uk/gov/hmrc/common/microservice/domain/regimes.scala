package uk.gov.hmrc.common.microservice.domain

import uk.gov.hmrc.common.microservice.epaye.domain.EpayeDomain.EpayeRoot
import uk.gov.hmrc.common.microservice.ct.domain.CtDomain.CtRoot
import uk.gov.hmrc.common.microservice.auth.domain.{Regimes, UserAuthority}
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.VatRoot
import uk.gov.hmrc.common.microservice.sa.domain.SaDomain.SaRoot
import uk.gov.hmrc.common.microservice.agent.AgentRoot

abstract class TaxRegime {

  def isAuthorised(regimes: Regimes): Boolean

  def unauthorisedLandingPage: String
}

abstract class RegimeRoot[I] {
  def identifier: I
}

case class User(userId: String,
                userAuthority: UserAuthority,
                regimes: RegimeRoots,
                nameFromGovernmentGateway: Option[String] = None,
                decryptedToken: Option[String]) {

  def oid = userId.substring(userId.lastIndexOf("/") + 1)

  def getPaye = regimes.paye.get

  def getSa = regimes.sa.get

  def getCt = regimes.ct.get

  def getVat = regimes.vat.get

  def getEPaye = regimes.epaye.get

}

case class RegimeRoots(paye: Option[PayeRoot] = None,
                       sa: Option[SaRoot] = None,
                       vat: Option[VatRoot] = None,
                       epaye: Option[EpayeRoot] = None,
                       ct: Option[CtRoot] = None,
                       agent: Option[AgentRoot] = None) {

  lazy val hasBusinessTaxRegime = sa.isDefined || vat.isDefined || epaye.isDefined || ct.isDefined
}





