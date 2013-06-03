package controllers.service

import java.net.URI

case class PersonalData(paye: Option[URI])

case class BusinessData(paye: Option[URI])

case class AuthorityData(uri: String, personal: Option[PersonalData], business: Option[BusinessData])

class Authority(auth: Auth = new Auth()) extends ResponseHandler {

  import scala.concurrent.Future

  def get(uri: String): Future[AuthorityData] = response[AuthorityData](auth.authority(uri))
}

object Authority {

  private val authority = new Authority()

  def apply = authority
}
