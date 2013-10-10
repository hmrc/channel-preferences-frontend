package models.agent.addClient

case class ConfirmClient(correctClient:Boolean, authorised: Boolean, internalClientReference: Option[String])
