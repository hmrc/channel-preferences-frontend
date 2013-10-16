package controllers.agent.registration

import controllers.agent.registration.AgentCompanyDetailsFormFields._
import controllers.agent.registration.AgentTypeAndLegalEntityFormFields._
import controllers.agent.registration.AgentContactDetailsFormFields._
import controllers.agent.registration.AgentProfessionalBodyMembershipFormFields._
import controllers.agent.registration.FormNames._
import controllers.common.validators.AddressFields._
import uk.gov.hmrc.utils.DateConverter
import uk.gov.hmrc.common.microservice.keystore.KeyStore
import uk.gov.hmrc.common.microservice.domain.Address
import models.agent.{ProfessionalBodyMembership, AgentRegistrationRequest, ContactDetails, CompanyDetails}

trait AgentMapper extends DateConverter {

  def toAgent(implicit keyStore: KeyStore[Map[String, String]]) = {

    val phNumbers = Map[String, String](
      landlineNumber -> companyData(phoneNumbers + "." + landlineNumber),
      mobileNumber -> companyData(phoneNumbers + "." + mobileNumber)
    )

    val websiteUrls = websiteUrlsData

    val companyDetails = CompanyDetails(
      companyName = companyData(companyName),
      emailAddress = companyData(email),
      saUtr = companyData(saUtr),
      registeredWithHMRC = companyData(registeredOnHMRC).toBoolean,
      mainAddress = companyAddressData(mainAddress),
      communicationAddress = companyAddressData(communicationAddress),
      principalAddress = companyAddressData(businessAddress),
      tradingName = optionalCompanyData(tradingName),
      numbers = phNumbers,
      websiteURLs = websiteUrls,
      ctUTR = optionalCompanyData(ctUtr),
      vatVRN = optionalCompanyData(vatVrn),
      payeEmpRef = optionalCompanyData(payeEmpRef),
      companyHouseNumber = optionalCompanyData(companyHouseNumber)
    )

    val contactDetails = ContactDetails(
      title = contactDetailsData(title),
      firstName = contactDetailsData(firstName),
      lastName = contactDetailsData(lastName),
      dob = contactDetailsData(dateOfBirth),
      nino = contactDetailsData(nino)
    )

    AgentRegistrationRequest(
      legalEntity = agentTypeData(legalEntity),
      agentType = agentTypeData(agentType),
      daytimeNumber = contactDetailsData(daytimePhoneNumber),
      mobileNumber = contactDetailsData(mobilePhoneNumber),
      emailAddress = contactDetailsData(emailAddress),
      contactDetails = contactDetails,
      companyDetails = companyDetails,
      professionalBodyMembership = professionalBodyData
    )
  }

  private def membershipData(field: String)(implicit keyStore: KeyStore[Map[String, String]]) = data(professionalBodyMembershipFormName, field)

  private def optionalCompanyData(field: String)(implicit keyStore: KeyStore[Map[String, String]]) = optionalData(companyDetailsFormName, field)

  private def companyData(field: String)(implicit keyStore: KeyStore[Map[String, String]]) = data(companyDetailsFormName, field)

  private def companyAddressData(field: String)(implicit keyStore: KeyStore[Map[String, String]]) = addressData(companyDetailsFormName, field)

  private def agentTypeData(field: String)(implicit keyStore: KeyStore[Map[String, String]]) = data(agentTypeAndLegalEntityFormName, field)

  private def contactDetailsData(field: String)(implicit keyStore: KeyStore[Map[String, String]]) = data(contactFormName, field)

  private def websiteUrlsData(implicit keyStore: KeyStore[Map[String, String]]) = companyData(website) match {
    case "" => List.empty
    case value => List(value)
  }

  private def professionalBodyData(implicit keyStore: KeyStore[Map[String, String]]) = {
    membershipData(qualifiedProfessionalBody) match {
      case "" => None
      case value => Some(ProfessionalBodyMembership(value, membershipData(qualifiedMembershipNumber)))
    }
  }

  private def addressData(formName: String, field: String)(implicit keyStore: KeyStore[Map[String, String]]) = Address(
    addressLine1 = data(formName, field + "." + addressLine1),
    addressLine2 = optionalData(formName, field + "." + addressLine2),
    addressLine3 = optionalData(formName, field + "." + addressLine3),
    addressLine4 = optionalData(formName, field + "." + addressLine4),
    postcode = optionalData(formName, field + "." + postcode)
  )

  private def data(formName: String, field: String)(implicit keyStore: KeyStore[Map[String, String]]) = keyStore.get(formName) match {
    case Some(x) => x.getOrElse(field, "")
    case _ => ""
  }

  private def optionalData(formName: String, field: String)(implicit keyStore: KeyStore[Map[String, String]]) = keyStore.data(formName).get(field)
}