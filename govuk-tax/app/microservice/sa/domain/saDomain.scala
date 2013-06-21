package microservice.sa.domain

import microservice.domain.RegimeRoot
import microservice.sa.SaMicroService

case class SaRoot(utr: String, links: Map[String, String]) extends RegimeRoot {

  //  def designatoryDetails(implicit saMicroService: SaMicroService) = {
  //    resourceFor[DesignatoryDetails]("designatoryDetails").getOrElse(None)
  //
  //  }

  def personalDetails(implicit saMicroService: SaMicroService): Option[SaPerson] = {
    links.get("personalDetails") match {
      case Some(uri) => saMicroService.person(uri)
      case _ => None
    }
  }

}
//case class DesignatoryDetails(links: Map[String, String]) {
//
//  def person(implicit saMicroService: SaMicroService) = {
//    resourceFor[Person]("person").getOrElse(None)
//  }
//
//  private def resourceFor[T](resource: String)(implicit saMicroService: SaMicroService, m: Manifest[T]): Option[T] = {
//    links.get(resource) match {
//      case Some(uri) => saMicroService.linkedResource[T](uri)
//      case _ => None
//    }
//  }
//}
//
//case class Person(name: String)

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