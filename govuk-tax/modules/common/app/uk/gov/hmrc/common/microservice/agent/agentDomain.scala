package uk.gov.hmrc.common.microservice.agent

import uk.gov.hmrc.common.microservice.domain.{TaxRegime, Address}
import uk.gov.hmrc.domain.{Uar, SaUtr}
import uk.gov.hmrc.utils.DateConverter
import uk.gov.hmrc.common.microservice.auth.domain.Regimes
import controllers.common.routes


object AgentRegime extends TaxRegime {
  override def isAuthorised(regimes: Regimes) = {
    regimes.agent.isDefined
  }

  override def unauthorisedLandingPage: String = routes.LoginController.login().url
}

case class AgentRoot(uar: Uar, clients: Map[String, String])

case class AgentRegistrationRequest(legalEntity: String,
  agentType: String,
  daytimeNumber: String,
  mobileNumber: String,
  emailAddress: String,
  contactDetails: ContactDetails,
  companyDetails: CompanyDetails,
  professionalBodyMembership: Option[ProfessionalBodyMembership])

case class CompanyDetails(companyName: String,
  emailAddress: String,
  saUtr: String,
  registeredWithHMRC: Boolean,
  mainAddress: Address,
  communicationAddress: Address,
  principalAddress: Address,
  tradingName: Option[String] = None,
  numbers: Map[String, String] = Map.empty,
  websiteURLs: List[String] = List.empty,
  ctUTR: Option[String] = None,
  vatVRN: Option[String] = None,
  payeEmpRef: Option[String] = None,
  companyHouseNumber: Option[String] = None)

case class ProfessionalBodyMembership(professionalBody: String, membershipNumber: String)

case class ContactDetails(title: String,
  firstName: String,
  lastName: String,
  dob: String,
  nino: String,
  middleName: Option[String] = None)


case class MatchingPerson(nino: String, firstName: Option[String], lastName: Option[String], dateOfBirth: Option[String]) {
  lazy val dobAsLocalDate = dateOfBirth.map(DateConverter.parseToLocalDate)
}
case class SearchRequest(nino: String, firstName: Option[String], lastName: Option[String], dateOfBirth: Option[String])
