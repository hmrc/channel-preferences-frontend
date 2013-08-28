package controllers.agent.registration

import uk.gov.hmrc.common.microservice.agent._
import uk.gov.hmrc.common.microservice.keystore.KeyStore
import controllers.agent.registration.AgentCompanyDetailsFormFields._
import controllers.agent.registration.AgentTypeAndLegalEntityFormFields._
import controllers.agent.registration.AgentContactDetailsFormFields._
import controllers.agent.registration.AgentProfessionalBodyMembershipFormFields._
import controllers.agent.registration.FormNames._

trait AgentMapper {

  def toAgent(implicit keyStore: KeyStore): Agent = {
    val profBodyMembership = new ProfessionalBodyMembership(membershipData(professionalBodyMembership + "." + professionalBody),
      membershipData(professionalBodyMembership + "." + membershipNumber))

    val phNumbers = Map[String, String](landlineNumber -> companyData(phoneNumbers + "." + landlineNumber),
      mobileNumber -> companyData(phoneNumbers + "." + mobileNumber))
    val websiteUrls = List[String](companyData(website))

    val companyDetails = new CompanyDetails(companyData(companyName), companyData(email), companyData(saUtr),
      companyData(registeredOnHMRC).toBoolean, new Address(companyData(mainAddress)),
      new Address(companyData(communicationAddress)), new Address(companyData(businessAddress)),
      optionalCompanyData(tradingName), phNumbers, websiteUrls, optionalCompanyData(ctUtr), optionalCompanyData(vatVrn),
      optionalCompanyData(payeEmpRef), optionalCompanyData(companyHouseNumber))

    new Agent(agentTypeData(legalEntity), agentTypeData(agentType), contactDetailsData(daytimePhoneNumber),
      contactDetailsData(mobilePhoneNumber), contactDetailsData(emailAddress), companyDetails, profBodyMembership, None, None)
  }

  private def membershipData(field: String)(implicit keyStore: KeyStore): String = {
    data(professionalBodyMembershipFormName, field)
  }

  private def optionalCompanyData(field: String)(implicit keyStore: KeyStore): Option[String] = {
    optionalData(companyDetailsFormName, field)
  }

  private def companyData(field: String)(implicit keyStore: KeyStore): String = {
    data(companyDetailsFormName, field)
  }

  private def agentTypeData(field: String)(implicit keyStore: KeyStore): String = {
    data(agentTypeAndLegalEntityFormName, field)
  }

  private def contactDetailsData(field: String)(implicit keyStore: KeyStore): String = {
    data(contactFormName, field)
  }

  private def data(formName: String, field: String)(implicit keyStore: KeyStore): String = {
    keyStore.get(formName) match {
      case Some(x) => x.getOrElse(field, "")
      case _ => ""
    }
  }

  private def optionalData(formName: String, field: String)(implicit keyStore: KeyStore): Option[String] = {
    keyStore.data(formName).get(field)
  }
}