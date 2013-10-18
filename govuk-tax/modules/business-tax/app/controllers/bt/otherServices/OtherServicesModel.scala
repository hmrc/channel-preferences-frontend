package controllers.bt.otherServices

import views.helpers.RenderableMessage

case class OnlineServicesEnrolment(linkToPortal: RenderableMessage)

case class BusinessTaxesRegistration(registrationLink: Option[RenderableMessage], hmrcWebsiteLink: RenderableMessage)

case class OtherServicesSummary(onlineServicesEnrolment: OnlineServicesEnrolment, businessTaxesRegistration: BusinessTaxesRegistration)
