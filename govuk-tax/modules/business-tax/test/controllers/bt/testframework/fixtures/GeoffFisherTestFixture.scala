package controllers.bt.testframework.fixtures

import org.joda.time.{Duration, DateTimeZone, DateTime}
import uk.gov.hmrc.domain.{Vrn, EmpRef, CtUtr, SaUtr}
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import uk.gov.hmrc.common.microservice.ct.domain.CtRoot
import uk.gov.hmrc.common.microservice.vat.domain.VatRoot
import uk.gov.hmrc.common.microservice.epaye.domain.{EpayeRoot, EpayeLinks}

trait GeoffFisherTestFixture extends BusinessUserFixture {

  override val currentTime: DateTime = new DateTime(2013, 9, 27, 15, 7, 22, 232, DateTimeZone.UTC)

  val saUtr = SaUtr("123456789012")
  val ctUtr = CtUtr("6666644444")
  val empRef = EmpRef("342", "sdfdsf")
  val vrn = Vrn("456345576")

  val saLinks = Map("something" -> s"/sa/some/path/$saUtr/stuff")
  val ctLinks = Map("something" -> s"/ct/some/path/in/ct/$ctUtr/dsffds")
  val epayeLinks = EpayeLinks(accountSummary = Some(s"/epaye/wherever/$empRef/blah"))
  val vatLinks = Map("something" -> s"/vat/inside/vat/$vrn/stuff")

  override val userId = "/auth/oid/geoff"

  override val saRoot = Some(SaRoot(saUtr, saLinks))
  override val ctRoot = Some(CtRoot(ctUtr, ctLinks))
  override val epayeRoot = Some(EpayeRoot(empRef, epayeLinks))
  override val vatRoot = Some(VatRoot(vrn, vatLinks))

  override val lastLoginTimestamp = Some(currentTime.minus(Duration.standardDays(14)))
  override val lastRequestTimestamp = Some(currentTime.minus(Duration.standardMinutes(1)))
  override val nameFromGovernmentGateway: Option[String] = Some("Geoffrey From Government Gateway")
  override val governmentGatewayToken: Option[String] = Some("someToken")

}