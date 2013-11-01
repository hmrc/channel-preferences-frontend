package controllers.bt.otherservices

import views.helpers.RenderableMessage

case class ManageYourTaxes(links: Seq[RenderableMessage])

case class OnlineServicesEnrolment(linkToPortal: RenderableMessage)

case class BusinessTaxesRegistration(registrationLink: Option[RenderableMessage], hmrcWebsiteLink: RenderableMessage)

case class OtherServicesSummary(manageYourTaxes: Option[ManageYourTaxes], onlineServicesEnrolment: OnlineServicesEnrolment, onlineServicesDeEnrolment: OnlineServicesEnrolment, businessTaxesRegistration: BusinessTaxesRegistration)
