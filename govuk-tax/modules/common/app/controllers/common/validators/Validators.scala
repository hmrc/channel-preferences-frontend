package controllers.common.validators

trait Validators {

  val phoneNumberErrorKey = "error.agent.phone"

  def validateMandatoryPhoneNumber = { s: String => s.matches("\\d+") }
  def validateOptionalPhoneNumber = { s: String => s.matches("\\d*") }
  def validateOptionalEmail = { s: String => s.isEmpty || s.matches("""\b[a-zA-Z0-9.!#$%&’*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*\b""") }
  def validateSaUtr = { s: String => s.matches("\\d{10}") }
}
