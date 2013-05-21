package controllers.service

import java.net.URI

case class TaxUserView(_id: URI, person: Option[URI] = None, company: Option[URI] = None, agent: Option[URI] = None)

class TaxUser(auth: Auth = new Auth()) extends ResponseHandler {

  import scala.concurrent.Future

  def get(pid: String): Future[TaxUserView] = response[TaxUserView](auth.taxUser(pid))
}

object TaxUser {

  private val taxUser = new TaxUser()

  def apply() = taxUser
}
