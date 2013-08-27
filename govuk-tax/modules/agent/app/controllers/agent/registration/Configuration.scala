package controllers.agent.registration

object Configuration {
  val agentTypeOptions: Map[String, String] = Map[String, String](
    "inBusiness" -> "In business as an agent",
    "unpaidAgentFamily" -> "Unpaid agent - Friends and family",
    "unpaidAgentVoluntary" -> "Unpaid agent - Voluntary and Community Sector",
    "employer" -> "Employer acting for employees"
  )

  val legalEntityOptions: Map[String, String] = Map[String, String](
    "ltdCompany" -> "Limited Company",
    "partnership" -> "Partnership (e.g. Ordinary Partnership, Limited Partnership, Limited Liability Partnership, Scottish Limited Partnership)",
    "soleProprietor" -> "Sole Proprietor"
  )

  val professionalBodyOptions: Map[String, String] = Map[String, String](
    "associationOfAccountingTechnicians" -> "Association of Accounting Technicians",
    "associationOfCharteredCertifiedAccountants" -> "Association of Chartered Certified Accountants",
    "associationOfInternationalAccountants" -> "Association of International Accountants",
    "associationOfTaxationTechnicians" -> "Association of Taxation Technicians",
    "charteredInstituteOfManagementAccountants" -> "Chartered Institute of Management Accountants",
    "charteredInstituteOfPublicFinanceandAccountancy" -> "Chartered Institute of Public Finance and Accountancy",
    "charteredInstituteOfTaxation" -> "Chartered Institute of Taxation",
    "councilForLicensedConveyors" -> "Council for Licensed Conveyors",
    "facultyOfAdvocates" -> "Faculty of Advocates",
    "facultyOfficeOfTheArchbishopOfCanterbury" -> "Faculty Office of the Archbishop of Canterbury",
    "generalCouncilOfTheBar" -> "General Council of the Bar",
    "generalCouncilOfTheBarOfNorthernIreland" -> "General Council of the Bar of Northern Ireland",
    "insolvencyPractitionersAssociation" -> "Insolvency Practitioners Association",
    "instituteOfCertifiedBookkeepers" -> "Institute of Certified Bookkeepers",
    "instituteOfCharteredAccountantsInEnglandAndWales" -> "Institute of Chartered Accountants in England and Wales",
    "instituteOfCharteredAccountantsInIreland" -> "Institute of Chartered Accountants in Ireland",
    "instituteOfCharteredAccountantsOfScotland" -> "Institute of Chartered Accountants of Scotland",
    "instituteOfFinancialAccountants" -> "Institute of Financial Accountants",
    "internationalAssociationOfBookkeepers" -> "International Association of Book-keepers",
    "lawSociety" -> "Law Society",
    "lawSocietyOfScotland" -> "Law Society of Scotland",
    "lawSocietyOfNorthernIreland" -> "Law Society of Northern Ireland"
  )

  val config = new Configuration();
}

case class Configuration(agentTypeOptions: Map[String, String] = Configuration.agentTypeOptions, legalEntityOptions: Map[String, String] = Configuration.legalEntityOptions,
  professionalBodyOptions: Map[String, String] = Configuration.professionalBodyOptions)
