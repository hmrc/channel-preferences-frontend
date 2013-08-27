package uk.gov.hmrc.common.microservice.agent

import org.joda.time.DateTime

case class AgentDTO(legalEntity: String,
  agentType: String,
  daytimeNumber: String,
  mobileNumber: String,
  emailAddress: String,
  companyDetails: CompanyDetailsDTO,
  professionalBodyMembership: ProfessionalBodyMembershipDTO,
  createdAt: Option[DateTime],
  uar: Option[String])

case class CompanyDetailsDTO(companyName: String,
  emailAddress: String,
  saUTR: String,
  registeredWithHMRC: Boolean,
  mainAddress: AddressDTO,
  communicationAddress: AddressDTO,
  principalAddress: AddressDTO,
  tradingName: Option[String] = None,
  phoneNumbers: Map[String, String] = Map.empty,
  websiteURLs: List[String] = List.empty,
  ctUTR: Option[String] = None,
  vatVRN: Option[String] = None,
  payeEmpRef: Option[String] = None,
  companyHouseNumber: Option[String] = None)

case class AddressDTO(addressLine1: String,
  addressLine2: Option[String] = None,
  addressLine3: Option[String] = None,
  addressLine4: Option[String] = None,
  postcode: Option[String] = None)

case class ProfessionalBodyMembershipDTO(professionalBody: String, membershipNumber: String)
