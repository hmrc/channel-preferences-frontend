package controllers.common.validators

import play.api.data.Forms._
import Validators._

trait Validators {

  val addressTuple = tuple(
    addressLine1 -> nonEmptyText,
    addressLine2 -> optional(text),
    addressLine3 -> optional(text),
    addressLine4 -> optional(text),
    postcode -> optional(text)
  )

  val phoneNumberErrorKey = "error.agent.phone"

  def validateMandatoryPhoneNumber = { s: String => s.matches("\\d+") }
  def validateOptionalPhoneNumber = { s: String => s.matches("\\d*") }
  def validateOptionalEmail = { s: String => s.isEmpty || s.matches("""\b[a-zA-Z0-9.!#$%&’*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*\b""") }
  def validateSaUtr = { s: String => s.matches("\\d{10}") }
}

object Validators {
  val addressLine1 = "addressLine1"
  val addressLine2 = "addressLine2"
  val addressLine3 = "addressLine3"
  val addressLine4 = "addressLine4"
  val postcode = "postcode"
}