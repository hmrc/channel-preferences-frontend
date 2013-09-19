package uk.gov.hmrc.common.microservice.agent

import org.joda.time.DateTime
import uk.gov.hmrc.common.microservice.domain.Address
import uk.gov.hmrc.domain.{CtUtr, SaUtr, Vrn, Nino}

case class Agent(legalEntity: String,
  agentType: String,
  daytimeNumber: String,
  mobileNumber: String,
  emailAddress: String,
  contactDetails: ContactDetails,
  companyDetails: CompanyDetails,
  professionalBodyMembership: Option[ProfessionalBodyMembership],
  createdAt: Option[DateTime],
  uar: Option[String])

case class CompanyDetails(companyName: String,
  emailAddress: String,
  saUtr: SaUtr,
  registeredWithHMRC: Boolean,
  mainAddress: Address,
  communicationAddress: Address,
  principalAddress: Address,
  tradingName: Option[String] = None,
  numbers: Map[String, String] = Map.empty,
  websiteURLs: List[String] = List.empty,
  ctUTR: Option[CtUtr] = None,
  vatVRN: Option[Vrn] = None,
  payeEmpRef: Option[String] = None,
  companyHouseNumber: Option[String] = None)

case class ProfessionalBodyMembership(professionalBody: String, membershipNumber: String)

case class ContactDetails(title: String,
  firstName: String,
  lastName: String,
  dob: Long,
  nino: Nino,
  middleName: Option[String] = None)
