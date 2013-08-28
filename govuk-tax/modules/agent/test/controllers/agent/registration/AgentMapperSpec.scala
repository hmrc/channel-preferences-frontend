package controllers.agent.registration

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import org.joda.time.DateTime
import scala.Predef._
import uk.gov.hmrc.common.microservice.keystore.KeyStore
import play.api.test.{ FakeApplication, WithApplication }
import controllers.agent.registration.AgentContactDetailsFormFields._
import controllers.agent.registration.AgentTypeAndLegalEntityFormFields._
import controllers.agent.registration.AgentCompanyDetailsFormFields._
import controllers.agent.registration.AgentProfessionalBodyMembershipFormFields._
import controllers.common.validators.Validators._

class AgentMapperSpec extends BaseSpec with MockitoSugar {

  class AgentMapperImpl extends AgentMapper {}
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
      agent.companyDetails.mainAddress.addressLine1 must be("Main Address l1")
      agent.companyDetails.mainAddress.addressLine2 must be(Some("Main Address l2"))
      agent.companyDetails.mainAddress.addressLine3 must be(Some("Main Address l3"))
      agent.companyDetails.mainAddress.addressLine4 must be(Some("Main Address l4"))
      agent.companyDetails.mainAddress.postcode must be(Some("Main Postcode"))
      agent.companyDetails.communicationAddress.addressLine1 must be("Communication Address l1")
      agent.companyDetails.communicationAddress.addressLine2 must be(Some("Communication Address l2"))
      agent.companyDetails.communicationAddress.addressLine3 must be(Some("Communication Address l3"))
      agent.companyDetails.communicationAddress.addressLine4 must be(Some("Communication Address l4"))
      agent.companyDetails.communicationAddress.postcode must be(Some("Communication Postcode"))
      agent.companyDetails.principalAddress.addressLine1 must be("Business Address l1")
      agent.companyDetails.principalAddress.addressLine2 must be(Some("Business Address l2"))
      agent.companyDetails.principalAddress.addressLine3 must be(Some("Business Address l3"))
      agent.companyDetails.principalAddress.addressLine4 must be(Some("Business Address l4"))
      agent.companyDetails.principalAddress.postcode must be(Some("Business Postcode"))
      agent.companyDetails.tradingName.get must be("Trading Name")
      agent.companyDetails.phoneNumbers(AgentCompanyDetailsFormFields.landlineNumber) must be("02073645362")
      agent.companyDetails.phoneNumbers(AgentCompanyDetailsFormFields.mobileNumber) must be("07777777771")
      agent.companyDetails.websiteURLs.head must be("www.agent.com")
      agent.companyDetails.ctUTR.get must be("CT UTR")
      agent.companyDetails.vatVRN.get must be("VAT Vrn")
      agent.companyDetails.payeEmpRef.get must be("PAYE Emp Ref")
      agent.companyDetails.companyHouseNumber.get must be("23")
      agent.professionalBodyMembership.get.professionalBody must be("profBody")
      agent.professionalBodyMembership.get.membershipNumber must be("888")
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
      agent.companyDetails.mainAddress.addressLine1 must be("Main Address l1")
      agent.companyDetails.communicationAddress.addressLine1 must be("Communication Address l1")
      agent.companyDetails.principalAddress.addressLine1 must be("Business Address l1")
      agent.companyDetails.tradingName must be(None)
      agent.companyDetails.phoneNumbers(AgentCompanyDetailsFormFields.landlineNumber) must be("02073645362")
      agent.companyDetails.phoneNumbers(AgentCompanyDetailsFormFields.mobileNumber) must be("")
      agent.companyDetails.websiteURLs.isEmpty must be(right = true)
      agent.companyDetails.ctUTR must be(None)
      agent.companyDetails.vatVRN must be(None)
      agent.companyDetails.payeEmpRef must be(None)
      agent.companyDetails.companyHouseNumber must be(None)
      agent.professionalBodyMembership must be(None)
      agent.createdAt must be(None)
      agent.uar must be(None)

    }

  }

  private def getKeyStoreWithAllInformation: KeyStore = {
    val data =
      Map(
        FormNames.contactFormName ->
          Map(
            daytimePhoneNumber -> "02000000000",
            mobilePhoneNumber -> "07777777777",
            emailAddress -> "agent@agent.com"
          ),
        FormNames.agentTypeAndLegalEntityFormName ->
          Map(
            legalEntity -> "legalEntityValue",
            agentType -> "agentTypeValue"
          ),
        FormNames.companyDetailsFormName ->
          Map(
            businessAddress + "." + addressLine1 -> "Business Address l1",
            businessAddress + "." + addressLine2 -> "Business Address l2",
            businessAddress + "." + addressLine3 -> "Business Address l3",
            businessAddress + "." + addressLine4 -> "Business Address l4",
            businessAddress + "." + postcode -> "Business Postcode",
            communicationAddress + "." + addressLine1 -> "Communication Address l1",
            communicationAddress + "." + addressLine2 -> "Communication Address l2",
            communicationAddress + "." + addressLine3 -> "Communication Address l3",
            communicationAddress + "." + addressLine4 -> "Communication Address l4",
            communicationAddress + "." + postcode -> "Communication Postcode",
            companyHouseNumber -> "23",
            companyName -> "Company Name LTD",
            ctUtr -> "CT UTR",
            email -> "company@company.com",
            qualifiedLandlineNumber -> "02073645362",
            mainAddress + "." + addressLine1 -> "Main Address l1",
            mainAddress + "." + addressLine2 -> "Main Address l2",
            mainAddress + "." + addressLine3 -> "Main Address l3",
            mainAddress + "." + addressLine4 -> "Main Address l4",
            mainAddress + "." + postcode -> "Main Postcode",
            qualifiedMobileNumber -> "07777777771",
            payeEmpRef -> "PAYE Emp Ref",
            registeredOnHMRC -> "true",
            saUtr -> "1234567890",
            tradingName -> "Trading Name",
            vatVrn -> "VAT Vrn",
            website -> "www.agent.com"
          ),
        FormNames.professionalBodyMembershipFormName ->
          Map(
            qualifiedMembershipNumber -> "888",
            qualifiedProfessionalBody -> "profBody"
          )
      )
    new KeyStore("1", new DateTime(), new DateTime(), data)
  }

  private def getKeyStoreWithOnlyMandatoryInformation: KeyStore = {
    val data =
      Map(
        FormNames.contactFormName ->
          Map(
            daytimePhoneNumber -> "02000000000",
            mobilePhoneNumber -> "07777777777",
            emailAddress -> "agent@agent.com"
          ),
        FormNames.agentTypeAndLegalEntityFormName ->
          Map(
            legalEntity -> "legalEntityValue",
            agentType -> "agentTypeValue"
          ),
        FormNames.companyDetailsFormName ->
          Map(
            businessAddress + "." + addressLine1 -> "Business Address l1",
            communicationAddress + "." + addressLine1 -> "Communication Address l1",
            companyName -> "Company Name LTD",
            email -> "company@company.com",
            qualifiedLandlineNumber -> "02073645362",
            mainAddress + "." + addressLine1 -> "Main Address l1",
            registeredOnHMRC -> "true",
            saUtr -> "1234567890"
          ),
        FormNames.professionalBodyMembershipFormName ->
          Map(
            qualifiedMembershipNumber -> "",
            qualifiedProfessionalBody -> ""
          )
      )
    new KeyStore("1", new DateTime(), new DateTime(), data)
  }

}
