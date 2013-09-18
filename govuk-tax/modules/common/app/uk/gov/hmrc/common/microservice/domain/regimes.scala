package uk.gov.hmrc.common.microservice.domain

import uk.gov.hmrc.common.microservice.auth.domain.{UserAuthority, Regimes}
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.VatRoot

abstract class TaxRegime {

  def isAuthorised(regimes: Regimes): Boolean

  def unauthorisedLandingPage: String
}

abstract class RegimeRoot

case class User(user: String,
                userAuthority: UserAuthority,
                regimes: RegimeRoots,
                nameFromGovernmentGateway: Option[String] = None,
                decryptedToken: Option[String]) {

  def oid: String = user.substring(user.lastIndexOf("/") + 1)

}

case class RegimeRoots(paye: Option[PayeRoot],
                       sa: Option[SaRoot],
                       vat: Option[VatRoot]) {

  lazy val hasBusinessTaxRegime: Boolean = sa.isDefined || vat.isDefined
}

//object RegimeRoots {
//
//  import uk.gov.hmrc.common.microservice.auth.AuthMicroService
//  import controllers.common.service.MicroServices
//  import uk.gov.hmrc.common.microservice.paye.PayeMicroService
//  import uk.gov.hmrc.common.microservice.sa.SaMicroService
//  import uk.gov.hmrc.common.microservice.vat.VatMicroService
//  import java.net.URI
//
//  val authMicroService: AuthMicroService = MicroServices.authMicroService
//  val paye: PayeMicroService = MicroServices.payeMicroService
//  val sa: SaMicroService = MicroServices.saMicroService
//  val vat: VatMicroService = MicroServices.vatMicroService
//
//  def apply(regimes: Regimes): RegimeRoots = RegimeRoots(
//    regimes.paye match {
//      case Some(x: URI) => Some(paye.root(x.toString))
//      case _ => None
//    },
//    regimes.sa match {
//      case Some(x: URI) => Some(sa.root(x.toString))
//      case _ => None
//    },
//    regimes.vat match {
//      case Some(x: URI) => Some(vat.root(x.toString))
//      case _ => None
//    }
//  )
//}
