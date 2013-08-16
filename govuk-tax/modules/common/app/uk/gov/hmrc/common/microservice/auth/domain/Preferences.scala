package uk.gov.hmrc.common.microservice.auth.domain

case class Preferences(sa: Option[SaPreferences])

case class SaPreferences(digitalNotifications: Option[Boolean], notificationEmail: Option[String])

