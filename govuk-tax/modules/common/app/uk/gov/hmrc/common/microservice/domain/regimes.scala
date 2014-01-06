package uk.gov.hmrc.common.microservice.domain

import uk.gov.hmrc.common.microservice.auth.domain.{Authority, Accounts}
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import controllers.common.AuthenticationProvider
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import uk.gov.hmrc.common.microservice.vat.domain.VatRoot
import uk.gov.hmrc.common.microservice.ct.domain.CtRoot
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeRoot

abstract class TaxRegime {

  def isAuthorised(accounts: Accounts): Boolean

  def unauthorisedLandingPage: String

  def authenticationType: AuthenticationProvider
}

abstract class RegimeRoot[I] {
  def identifier: I
}

case class User(userId: String,
                userAuthority: Authority,
                regimes: RegimeRoots,
                nameFromGovernmentGateway: Option[String] = None,
                decryptedToken: Option[String] = None) {

  def oid = userId.substring(userId.lastIndexOf("/") + 1)

  def getPaye = regimes.paye.get

  def getSa = regimes.sa.get

  def getCt = regimes.ct.get

  def getVat = regimes.vat.get

  def getEPaye = regimes.epaye.get

  def displayName : Option[String] = {
    regimes.paye.map(_.name).orElse(nameFromGovernmentGateway)
  }

}

case class RegimeRoots(paye: Option[PayeRoot] = None,
                       sa: Option[SaRoot] = None,
                       vat: Option[VatRoot] = None,
                       epaye: Option[EpayeRoot] = None,
                       ct: Option[CtRoot] = None) {

  lazy val hasBusinessTaxRegime = sa.isDefined || vat.isDefined || epaye.isDefined || ct.isDefined
}





