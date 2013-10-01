package uk.gov.hmrc.common.microservice.domain

import uk.gov.hmrc.common.microservice.auth.domain.{UserAuthority, Regimes}
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.VatRoot
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeDomain.EpayeRoot
import uk.gov.hmrc.common.microservice.ct.domain.CtDomain.CtRoot

abstract class TaxRegime {

  def isAuthorised(regimes: Regimes): Boolean

  def unauthorisedLandingPage: String
}

abstract class RegimeRoot[I] {
  def identifier  : I
}

case class User(userId: String,
                userAuthority: UserAuthority,
                regimes: RegimeRoots,
                nameFromGovernmentGateway: Option[String] = None,
                decryptedToken: Option[String]) {

  def oid: String = userId.substring(userId.lastIndexOf("/") + 1)

}

case class RegimeRoots(paye: Option[PayeRoot],
                       sa: Option[SaRoot],
                       vat: Option[VatRoot],
                       epaye: Option[EpayeRoot],
                       ct: Option[CtRoot]
                        ) {

  lazy val hasBusinessTaxRegime: Boolean = sa.isDefined || vat.isDefined || epaye.isDefined
}
