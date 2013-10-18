package controllers.bt.mixins.fixtures

import org.joda.time.DateTime
import uk.gov.hmrc.common.microservice.sa.domain.SaDomain.SaRoot
import uk.gov.hmrc.common.microservice.ct.domain.CtDomain.CtRoot
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeDomain.EpayeRoot
import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.VatRoot

trait UserFixture {

  def userId: String
  def currentTime: DateTime
  def lastRequestTimestamp: Option[DateTime]
  def lastLoginTimestamp: Option[DateTime]
}

trait BusinessUserFixture extends UserFixture {

  def saRoot: Option[SaRoot]
  def ctRoot: Option[CtRoot]
  def epayeRoot: Option[EpayeRoot]
  def vatRoot: Option[VatRoot]

  def affinityGroup: Option[String]
  def nameFromGovernmentGateway: Option[String]
  def governmentGatewayToken: Option[String]
}

trait NonBusinessUserFixture extends UserFixture
