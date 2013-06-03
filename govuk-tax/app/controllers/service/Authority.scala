package controllers.service

import java.net.URI

case class PersonalData(paye: Option[URI], sa: Option[URI])

case class BusinessData(paye: Option[URI], vat: Option[URI], ct: Option[URI])

case class AuthorityData(id: String, personal: Option[PersonalData], business: Option[BusinessData])

case class AuthorityView(regimes: List[Regime])

object RegimeSubject extends Enumeration {
  type RegimeSubject = Value
  val Personal, Business = Value
}

case class Regime(val name: String, val subject: RegimeSubject.type, val uri: URI)

class Authority(auth: Auth = new Auth()) extends ResponseHandler {

  import scala.concurrent.Future

  def get(uri: String): Future[AuthorityData] = response[AuthorityData](auth.authority(uri))
}

object Authority {

  private val authority = new Authority()

  def apply = authority
}
