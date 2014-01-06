package uk.gov.hmrc.common.microservice.auth.domain

import org.joda.time.DateTime
import uk.gov.hmrc.domain._
import uk.gov.hmrc.domain.Uar
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.utils.DateTimeUtils


case class Authority(uri: String,
                     credentials: Credentials,
                     accounts: Accounts,
                     loggedInAt: Option[DateTime],
                     previouslyLoggedInAt: Option[DateTime],
                     crudDetail: CreationAndLastModifiedDetail)

case class IdaPid(pid: String, createdAt: DateTime, lastUsed: DateTime)

case class Credentials(gatewayId: Option[String] = None,
                       idaPids: Set[IdaPid] = Set.empty)

case class Accounts(paye: Option[PayeAccount] = None,
                    sa: Option[SaAccount] = None,
                    ct: Option[CtAccount] = None,
                    vat: Option[VatAccount] = None,
                    epaye: Option[EpayeAccount] = None) {
  def toMap = Map() ++
    sa.map("saUtr" -> _.utr.utr).toMap ++
    vat.map("vrn" -> _.vrn.vrn).toMap ++
    ct.map("ctUtr" -> _.utr.utr).toMap ++
    epaye.map("empRef" -> _.empRef.toString).toMap ++
    paye.map("nino" -> _.nino.nino).toMap
}

case class PayeAccount(link: String, nino: Nino) extends Account

case class SaAccount(link: String, utr: SaUtr) extends Account

case class CtAccount(link: String, utr: CtUtr) extends Account

case class VatAccount(link: String, vrn: Vrn) extends Account

case class EpayeAccount(link: String, empRef: EmpRef) extends Account

sealed abstract class Account {


  val link: String
}

case class CreationAndLastModifiedDetail(createdAt: DateTime, lastUpdated: DateTime)
object CreationAndLastModifiedDetail {
  def apply(): CreationAndLastModifiedDetail = CreationAndLastModifiedDetail(DateTimeUtils.now, DateTimeUtils.now)
}