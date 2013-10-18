package controllers.bt.mixins.fixtures

import org.joda.time.{Duration, DateTimeZone, DateTime}
import uk.gov.hmrc.domain.{Vrn, EmpRef, CtUtr, SaUtr}
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeDomain.{EpayeRoot, EpayeLinks}
import uk.gov.hmrc.common.microservice.sa.domain.SaDomain.SaRoot
import uk.gov.hmrc.common.microservice.ct.domain.CtDomain.CtRoot
import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.VatRoot

trait GeoffFisherTestFixture extends BusinessUserFixture {

  override val currentTime: DateTime = new DateTime(2013, 9, 27, 15, 7, 22, 232, DateTimeZone.UTC)

  val saUtr = SaUtr("/sa/individual/123456789012")
  val ctUtr = CtUtr("/ct/6666644444")
  val empRef = EmpRef("342", "sdfdsf")
  val vrn = Vrn("456345576")

  val saLinks = Map("something" -> s"$saUtr/stuff")
  val ctLinks = Map("something" -> s"$ctUtr/dsffds")
  val epayeLinks = EpayeLinks(accountSummary = Some(s"$empRef/blah"))
  val vatLinks = Map("something" -> s"$vrn/stuff")

  override val userId = "/auth/oid/geoff"

  override val saRoot = Some(SaRoot(saUtr, saLinks))
  override val ctRoot = Some(CtRoot(ctUtr, ctLinks))
  override val epayeRoot = Some(EpayeRoot(empRef, epayeLinks))
  override val vatRoot = Some(VatRoot(vrn, vatLinks))

  override val lastLoginTimestamp = Some(currentTime.minus(Duration.standardDays(14)))
  override val lastRequestTimestamp = Some(currentTime.minus(Duration.standardMinutes(1)))
  override val affinityGroup = Some("someAffinityGroup")
  override val nameFromGovernmentGateway: Option[String] = Some("Geoffrey From Government Gateway")
  override val governmentGatewayToken: Option[String] = Some("someToken")

}