package controllers.bt.testframework.fixtures

import org.joda.time.DateTime
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import uk.gov.hmrc.common.microservice.ct.domain.CtRoot
import uk.gov.hmrc.common.microservice.vat.domain.VatRoot
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeRoot
import uk.gov.hmrc.common.microservice.auth.domain.Authority

trait UserFixture {

  def userId: String
  def currentTime: DateTime
  def lastRequestTimestamp: Option[DateTime]
  def lastLoginTimestamp: Option[DateTime]
  def authority: Authority
}

trait BusinessUserFixture extends UserFixture {

  def saRoot: Option[SaRoot]
  def ctRoot: Option[CtRoot]
  def epayeRoot: Option[EpayeRoot]
  def vatRoot: Option[VatRoot]

  def nameFromGovernmentGateway: Option[String]
  def governmentGatewayToken: Option[String]
}

trait NonBusinessUserFixture extends UserFixture
