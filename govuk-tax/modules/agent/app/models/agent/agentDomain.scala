package models.agent

import uk.gov.hmrc.common.microservice.domain.Address
import uk.gov.hmrc.utils.DateConverter


case class Client(nino: String, internalReferenceNumber: Option[String], preferredContact: PreferredContact)
case class PreferredContact(isUs: Boolean, preferredContactForUs: Option[Contact])
case class Contact(altName:String, altEmail:String, altPhone:String)


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
