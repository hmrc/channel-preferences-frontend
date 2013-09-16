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
import controllers.common.validators.AddressFields._
import views.formatting.DatesSpec
import uk.gov.hmrc.utils.DateConverter
import uk.gov.hmrc.domain.{Vrn, Utr, Nino}

class AgentMapperSpec extends BaseSpec with MockitoSugar with DateConverter {

  class AgentMapperImpl extends AgentMapper {}
  def agentMapper = new AgentMapperImpl()

  "AgentMapper" should {

    "create an agent when all the information is provided by the keystore" in new WithApplication(FakeApplication()) {

      val agent = agentMapper.toAgent(getKeyStoreWithAllInformation)

      agent.contactDetails.title should be("Miss")
      agent.contactDetails.firstName should be("Nat")
      agent.contactDetails.lastName should be("Butterfly")
      agent.contactDetails.dob should be(parseToLong("1985-09-02"))
      agent.contactDetails.nino should be(Nino("AB763527J"))
      agent.legalEntity should be("legalEntityValue")
      agent.agentType should be("agentTypeValue")
      agent.daytimeNumber should be("02000000000")
      agent.mobileNumber should be("07777777777")
      agent.emailAddress should be("agent@agent.com")
      agent.companyDetails.companyName should be("Company Name LTD")
      agent.companyDetails.emailAddress should be("company@company.com")
      agent.companyDetails.saUtr should be(Utr("1234567890"))
      agent.companyDetails.registeredWithHMRC should be(right = true)
      agent.companyDetails.mainAddress.addressLine1 should be("Main Address l1")
      agent.companyDetails.mainAddress.addressLine2 should be(Some("Main Address l2"))
      agent.companyDetails.mainAddress.addressLine3 should be(Some("Main Address l3"))
      agent.companyDetails.mainAddress.addressLine4 should be(Some("Main Address l4"))
      agent.companyDetails.mainAddress.postcode should be(Some("Main Postcode"))
      agent.companyDetails.communicationAddress.addressLine1 should be("Communication Address l1")
      agent.companyDetails.communicationAddress.addressLine2 should be(Some("Communication Address l2"))
      agent.companyDetails.communicationAddress.addressLine3 should be(Some("Communication Address l3"))
      agent.companyDetails.communicationAddress.addressLine4 should be(Some("Communication Address l4"))
      agent.companyDetails.communicationAddress.postcode should be(Some("Communication Postcode"))
      agent.companyDetails.principalAddress.addressLine1 should be("Business Address l1")
      agent.companyDetails.principalAddress.addressLine2 should be(Some("Business Address l2"))
      agent.companyDetails.principalAddress.addressLine3 should be(Some("Business Address l3"))
      agent.companyDetails.principalAddress.addressLine4 should be(Some("Business Address l4"))
      agent.companyDetails.principalAddress.postcode should be(Some("Business Postcode"))
      agent.companyDetails.tradingName.get should be("Trading Name")
      agent.companyDetails.numbers(AgentCompanyDetailsFormFields.landlineNumber) should be("02073645362")
      agent.companyDetails.numbers(AgentCompanyDetailsFormFields.mobileNumber) should be("07777777771")
      agent.companyDetails.websiteURLs.head should be("www.agent.com")
      agent.companyDetails.ctUTR.get should be(Utr("CT UTR"))
      agent.companyDetails.vatVRN.get should be(Vrn("VAT Vrn"))
      agent.companyDetails.payeEmpRef.get should be("PAYE Emp Ref")
      agent.companyDetails.companyHouseNumber.get should be("23")
      agent.professionalBodyMembership.get.professionalBody should be("profBody")
      agent.professionalBodyMembership.get.membershipNumber should be("888")
      agent.createdAt should be(None)
      agent.uar should be(None)

    }

    "create an agent when only mandatory information is provided by the keystore" in new WithApplication(FakeApplication()) {

      val agent = agentMapper.toAgent(getKeyStoreWithOnlyMandatoryInformation)

      agent.contactDetails.title should be("Miss")
      agent.contactDetails.firstName should be("Nat")
      agent.contactDetails.lastName should be("Butterfly")
      agent.contactDetails.dob should be(parseToLong("1985-09-02"))
      agent.contactDetails.nino should be(Nino("AB763527J"))
      agent.legalEntity should be("legalEntityValue")
      agent.agentType should be("agentTypeValue")
      agent.daytimeNumber should be("02000000000")
      agent.mobileNumber should be("07777777777")
      agent.emailAddress should be("agent@agent.com")
      agent.companyDetails.companyName should be("Company Name LTD")
      agent.companyDetails.emailAddress should be("company@company.com")
      agent.companyDetails.saUtr should be(Utr("1234567890"))
      agent.companyDetails.registeredWithHMRC should be(right = true)
      agent.companyDetails.mainAddress.addressLine1 should be("Main Address l1")
      agent.companyDetails.communicationAddress.addressLine1 should be("Communication Address l1")
      agent.companyDetails.principalAddress.addressLine1 should be("Business Address l1")
      agent.companyDetails.tradingName should be(None)
      agent.companyDetails.numbers(AgentCompanyDetailsFormFields.landlineNumber) should be("02073645362")
      agent.companyDetails.numbers(AgentCompanyDetailsFormFields.mobileNumber) should be("")
      agent.companyDetails.websiteURLs.isEmpty should be(right = true)
      agent.companyDetails.ctUTR should be(None)
      agent.companyDetails.vatVRN should be(None)
      agent.companyDetails.payeEmpRef should be(None)
      agent.companyDetails.companyHouseNumber should be(None)
      agent.professionalBodyMembership should be(None)
      agent.createdAt should be(None)
      agent.uar should be(None)

    }

  }

  private def getKeyStoreWithAllInformation: KeyStore[Map[String, String]] = {
    val data =
      Map(
        FormNames.contactFormName ->
          Map(
            title -> "Miss",
            firstName -> "Nat",
            lastName -> "Butterfly",
            dateOfBirth -> "1985-09-02",
            nino -> "AB763527J",
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

  private def getKeyStoreWithOnlyMandatoryInformation: KeyStore[Map[String, String]] = {
    val data =
      Map(
        FormNames.contactFormName ->
          Map(
            title -> "Miss",
            firstName -> "Nat",
            lastName -> "Butterfly",
            dateOfBirth -> "1985-09-02",
            nino -> "AB763527J",
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
