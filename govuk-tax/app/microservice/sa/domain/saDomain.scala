package microservice.sa.domain

import microservice.domain.{ TaxRegime, RegimeRoot }
import microservice.sa.SaMicroService
import microservice.auth.domain.Regimes
import play.api.mvc.{ Call, AnyContent, Action }
import controllers.routes

object SaRegime extends TaxRegime {
  override def isAuthorised(regimes: Regimes) = {
    regimes.sa.isDefined
  }

  override def unauthorisedLandingPage: Call = {
    routes.SaController.noEnrolment()
  }
}

case class SaRoot(utr: String, links: Map[String, String]) extends RegimeRoot {

  def personalDetails(implicit saMicroService: SaMicroService): Option[SaPerson] = {
    links.get("personalDetails") match {
      case Some(uri) => saMicroService.person(uri)
      case _ => None
    }
  }

}

case class SaTaxData(utr: String, links: Map[String, String])

case class SaPerson(name: String, utr: String, address: SaIndividualAddress)

case class SaIndividualAddress(
  addressLine1: String,
  addressLine2: String,
  addressLine3: String,
  addressLine4: String,
  addressLine5: String,
  postcode: String,
  foreignCountry: String,
  additionalDeliveryInformation: String)