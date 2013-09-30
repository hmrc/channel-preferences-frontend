package controllers.agent.registration

import views.helpers.{InputType, RadioButton}

object Configuration {
  val agentTypeOptions = Seq[InputType](
    RadioButton("inBusiness", "In business as an agent"),
    RadioButton("unpaidAgentFamily", "Unpaid agent - Friends and family"),
    RadioButton("unpaidAgentVoluntary", "Unpaid agent - Voluntary and Community Sector"),
    RadioButton("employer", "Employer acting for employees")
  )

  val legalEntityOptions = Seq[InputType](
    RadioButton("ltdCompany", "Limited Company"),
    RadioButton("partnership", "Partnership (e.g. Ordinary Partnership, Limited Partnership, Limited Liability Partnership, Scottish Limited Partnership)"),
    RadioButton("soleProprietor", "Sole Proprietor")
  )

  val professionalBodyOptions: Seq[(String, String)] = Seq[(String, String)](
    "associationOfAccountingTechnicians" -> "Association of Accounting Technicians",
    "associationOfCharteredCertifiedAccountants" -> "Association of Chartered Certified Accountants",
    "associationOfInternationalAccountants" -> "Association of International Accountants",
    "associationOfTaxationTechnicians" -> "Association of Taxation Technicians",
    "charteredInstituteOfManagementAccountants" -> "Chartered Institute of Management Accountants",
    "charteredInstituteOfPublicFinanceandAccountancy" -> "Chartered Institute of Public Finance and Accountancy",
    "charteredInstituteOfTaxation" -> "Chartered Institute of Taxation",
    "instituteOfCertifiedBookkeepers" -> "Institute of Certified Bookkeepers",
    "instituteOfCharteredAccountantsInEnglandAndWales" -> "Institute of Chartered Accountants in England and Wales",
    "instituteOfCharteredAccountantsInIreland" -> "Institute of Chartered Accountants in Ireland",
    "instituteOfCharteredAccountantsOfScotland" -> "Institute of Chartered Accountants of Scotland",
    "instituteOfFinancialAccountants" -> "Institute of Financial Accountants",
    "internationalAssociationOfBookkeepers" -> "International Association of Book-keepers"
  )

  val config = new Configuration();
}

case class Configuration(agentTypeOptions: Seq[InputType] = Configuration.agentTypeOptions, legalEntityOptions: Seq[InputType] = Configuration.legalEntityOptions,
  professionalBodyOptions: Seq[(String, String)] = Configuration.professionalBodyOptions)
