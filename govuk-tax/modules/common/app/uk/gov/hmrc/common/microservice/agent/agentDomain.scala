package uk.gov.hmrc.common.microservice.agent

import org.joda.time.DateTime

case class Agent(legalEntity: String,
  agentType: String,
  daytimeNumber: String,
  mobileNumber: String,
  emailAddress: String,
  companyDetails: CompanyDetails,
  professionalBodyMembership: ProfessionalBodyMembership,
  createdAt: Option[DateTime],
  uar: Option[String])

case class CompanyDetails(companyName: String,
  emailAddress: String,
  saUTR: String,
  registeredWithHMRC: Boolean,
  mainAddress: Address,
  communicationAddress: Address,
  principalAddress: Address,
  tradingName: Option[String] = None,
  phoneNumbers: Map[String, String] = Map.empty,
  websiteURLs: List[String] = List.empty,
  ctUTR: Option[String] = None,
  vatVRN: Option[String] = None,
  payeEmpRef: Option[String] = None,
  companyHouseNumber: Option[String] = None)

//FIXME: address should be common, if not then rename to Agent specific address
case class Address(addressLine1: String,
  addressLine2: Option[String] = None,
  addressLine3: Option[String] = None,
  addressLine4: Option[String] = None,
  postcode: Option[String] = None)

case class ProfessionalBodyMembership(professionalBody: String, membershipNumber: String)
