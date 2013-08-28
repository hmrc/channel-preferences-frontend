package controllers.agent.registration

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import org.joda.time.DateTime
import scala.Predef._
import uk.gov.hmrc.common.microservice.keystore.KeyStore
import play.api.test.{FakeApplication, WithApplication}

class AgentMapperSpec extends BaseSpec with MockitoSugar {

  class AgentMapperImpl extends AgentMapper{}
  def agentMapper = new AgentMapperImpl()

  "AgentMapper" should {

    "create an agent when all the information is provided by the keystore" in new WithApplication(FakeApplication()) {

        val agent = agentMapper.toAgent(getKeyStoreWithAllInformation)

        agent.legalEntity must be("legalEntityValue")
        agent.agentType must be("agentTypeValue")
        agent.daytimeNumber must be("02000000000")
        agent.mobileNumber must be("07777777777")
        agent.emailAddress must be("agent@agent.com")
        agent.companyDetails.companyName must be("Company Name LTD")
        agent.companyDetails.emailAddress must be("company@company.com")
        agent.companyDetails.saUTR must be("1234567890")
        agent.companyDetails.registeredWithHMRC must be(right = true)
        agent.companyDetails.mainAddress.addressLine1 must be("Main Address")
        agent.companyDetails.communicationAddress.addressLine1 must be("Communication Address")
        agent.companyDetails.principalAddress.addressLine1 must be("Business Address")
        agent.companyDetails.tradingName.get must be("Trading Name")
        agent.companyDetails.phoneNumbers(AgentCompanyDetailsFormFields.landlineNumber) must be("02073645362")
        agent.companyDetails.phoneNumbers(AgentCompanyDetailsFormFields.mobileNumber) must be("07777777771")
        agent.companyDetails.websiteURLs.head must be("www.agent.com")
        agent.companyDetails.ctUTR.get must be("CT UTR")
        agent.companyDetails.vatVRN.get must be("VAT Vrn")
        agent.companyDetails.payeEmpRef.get must be("PAYE Emp Ref")
        agent.companyDetails.companyHouseNumber.get must be("23")
        agent.professionalBodyMembership.professionalBody must be("profBody")
        agent.professionalBodyMembership.membershipNumber must be("888")
        agent.createdAt must be(None)
        agent.uar must be(None)

    }

    "create an agent when only mandatory information is provided by the keystore" in new WithApplication(FakeApplication()) {

      val agent = agentMapper.toAgent(getKeyStoreWithOnlyMandatoryInformation)

      agent.legalEntity must be("legalEntityValue")
      agent.agentType must be("agentTypeValue")
      agent.daytimeNumber must be("02000000000")
      agent.mobileNumber must be("07777777777")
      agent.emailAddress must be("agent@agent.com")
      agent.companyDetails.companyName must be("Company Name LTD")
      agent.companyDetails.emailAddress must be("company@company.com")
      agent.companyDetails.saUTR must be("1234567890")
      agent.companyDetails.registeredWithHMRC must be(right = true)
      agent.companyDetails.mainAddress.addressLine1 must be("Main Address")
      agent.companyDetails.communicationAddress.addressLine1 must be("Communication Address")
      agent.companyDetails.principalAddress.addressLine1 must be("Business Address")
      agent.companyDetails.tradingName must be(None)
      agent.companyDetails.phoneNumbers(AgentCompanyDetailsFormFields.landlineNumber) must be("02073645362")
      agent.companyDetails.phoneNumbers(AgentCompanyDetailsFormFields.mobileNumber) must be("")
      agent.companyDetails.websiteURLs.isEmpty must be(right = true)
      agent.companyDetails.ctUTR must be(None)
      agent.companyDetails.vatVRN must be(None)
      agent.companyDetails.payeEmpRef must be(None)
      agent.companyDetails.companyHouseNumber must be(None)
      agent.professionalBodyMembership.professionalBody must be("profBody")
      agent.professionalBodyMembership.membershipNumber must be("888")
      agent.createdAt must be(None)
      agent.uar must be(None)

    }

  }

  private def getKeyStoreWithAllInformation: KeyStore = {
    val data =
      Map(
        FormNames.contactFormName ->
          Map(
            AgentContactDetailsFormFields.daytimePhoneNumber -> "02000000000",
            AgentContactDetailsFormFields.mobilePhoneNumber -> "07777777777",
            AgentContactDetailsFormFields.emailAddress -> "agent@agent.com"
          ),
        FormNames.agentTypeAndLegalEntityFormName ->
          Map(
            AgentTypeAndLegalEntityFormFields.legalEntity -> "legalEntityValue",
            AgentTypeAndLegalEntityFormFields.agentType -> "agentTypeValue"
          ),
        FormNames.companyDetailsFormName ->
          Map(
            AgentCompanyDetailsFormFields.businessAddress -> "Business Address",
            AgentCompanyDetailsFormFields.communicationAddress -> "Communication Address",
            AgentCompanyDetailsFormFields.companyHouseNumber -> "23",
            AgentCompanyDetailsFormFields.companyName -> "Company Name LTD",
            AgentCompanyDetailsFormFields.ctUtr -> "CT UTR",
            AgentCompanyDetailsFormFields.email -> "company@company.com",
            AgentCompanyDetailsFormFields.qualifiedLandlineNumber -> "02073645362",
            AgentCompanyDetailsFormFields.mainAddress -> "Main Address",
            AgentCompanyDetailsFormFields.qualifiedMobileNumber -> "07777777771",
            AgentCompanyDetailsFormFields.payeEmpRef -> "PAYE Emp Ref",
            AgentCompanyDetailsFormFields.registeredOnHMRC -> "true",
            AgentCompanyDetailsFormFields.saUtr -> "1234567890",
            AgentCompanyDetailsFormFields.tradingName -> "Trading Name",
            AgentCompanyDetailsFormFields.vatVrn -> "VAT Vrn",
            AgentCompanyDetailsFormFields.website -> "www.agent.com"
          ),
        FormNames.professionalBodyMembershipFormName ->
          Map(
            AgentProfessionalBodyMembershipFormFields.qualifiedMembershipNumber -> "888",
            AgentProfessionalBodyMembershipFormFields.qualifiedProfessionalBody-> "profBody"
          )
      )
    new KeyStore("1",new DateTime(),new DateTime(), data)
  }

  private def getKeyStoreWithOnlyMandatoryInformation: KeyStore = {
    val data =
      Map(
        FormNames.contactFormName ->
          Map(
            AgentContactDetailsFormFields.daytimePhoneNumber -> "02000000000",
            AgentContactDetailsFormFields.mobilePhoneNumber -> "07777777777",
            AgentContactDetailsFormFields.emailAddress -> "agent@agent.com"
          ),
        FormNames.agentTypeAndLegalEntityFormName ->
          Map(
            AgentTypeAndLegalEntityFormFields.legalEntity -> "legalEntityValue",
            AgentTypeAndLegalEntityFormFields.agentType -> "agentTypeValue"
          ),
        FormNames.companyDetailsFormName ->
          Map(
            AgentCompanyDetailsFormFields.businessAddress -> "Business Address",
            AgentCompanyDetailsFormFields.communicationAddress -> "Communication Address",
            AgentCompanyDetailsFormFields.companyName -> "Company Name LTD",
            AgentCompanyDetailsFormFields.email -> "company@company.com",
            AgentCompanyDetailsFormFields.qualifiedLandlineNumber -> "02073645362",
            AgentCompanyDetailsFormFields.mainAddress -> "Main Address",
            AgentCompanyDetailsFormFields.registeredOnHMRC -> "true",
            AgentCompanyDetailsFormFields.saUtr -> "1234567890"
          ),
        FormNames.professionalBodyMembershipFormName ->
          Map(
            AgentProfessionalBodyMembershipFormFields.qualifiedMembershipNumber -> "888",
            AgentProfessionalBodyMembershipFormFields.qualifiedProfessionalBody-> "profBody"
          )
      )
    new KeyStore("1",new DateTime(),new DateTime(), data)
  }

}
