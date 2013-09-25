package controllers.common.validators

import play.api.data.Forms._
import AddressFields._
import DateFields._
import scala.util.matching.Regex
import org.joda.time.{IllegalFieldValueException, LocalDate}
import play.api.data.{FormError, FieldMapping, Mapping}
import play.api.data.format.Formatter
import play.api.data.validation._
import play.api.data.FormError
import scala.Some
import scala.Some
import scala.Some

trait Validators {

  val dateTuple: Mapping[Option[LocalDate]] = dateTuple(true)

  def dateTuple(validate: Boolean = true, default: Option[LocalDate] = None) = tuple(
    year -> optional(text),
    month -> optional(text),
    day -> optional(text)
  ).verifying("error.invalid.date.format", data => {

    (data._1, data._2, data._3) match {
      case (None, None, None) => true
      case (year, month, day) => {
        try {
          new LocalDate(year.getOrElse(throw new Exception("Year missing")).toInt, month.getOrElse(throw new Exception("Month missing")).toInt, day.getOrElse(throw new Exception("Day missing")).toInt)
          true
        } catch {
          case _ => if (validate) {
            false
          } else {
            true
          }
        }
      }
    }

  }).transform(
  {
    case (Some(year), Some(month), Some(day)) => {
      try {
        Some(new LocalDate(year.toInt, month.toInt, day.toInt))
      } catch {
        case e: Exception => {
          if (validate) {
            throw e
          } else {
            default
          }
        }
      }
    }
    case (a, b, c) => default
  },
  (date: Option[LocalDate]) => date match {
    case Some(d) => (Some(d.getYear.toString), Some(d.getMonthOfYear.toString), Some(d.getDayOfMonth.toString))
    case _ => (None, None, None)
  }
  )

  val addressTuple = tuple(
    addressLine1 -> smallText
      .verifying("error.address.blank", e => notBlank(e))
      .verifying("error.address.main.line.max.length.violation", e => isMainAddressLineLengthValid(e))
      .verifying("error.address.invalid.character", e => characterValidator.containsValidAddressCharacters(e)),
    addressLine2 -> optional(smallText.verifying("error.address.optional.line.max.length.violation", e => isOptionalAddressLineLengthValid(e))
      .verifying("error.address.invalid.character", e => characterValidator.containsValidAddressCharacters(e))),
    addressLine3 -> optional(smallText.verifying("error.address.optional.line.max.length.violation", e => isOptionalAddressLineLengthValid(e))
      .verifying("error.address.invalid.character", e => characterValidator.containsValidAddressCharacters(e))),
    addressLine4 -> optional(smallText.verifying("error.address.optional.line.max.length.violation", e => isOptionalAddressLineLengthValid(e))
      .verifying("error.address.invalid.character", e => characterValidator.containsValidAddressCharacters(e))),
    postcode -> optional(smallText.verifying("error.postcode.length.violation", e => isPostcodeLengthValid(e))
      .verifying("error.postcode.invalid.character", e => characterValidator.containsValidPostCodeCharacters(e)))
  )

  // Small text prevents injecting large data into fields
  def smallText = play.api.data.Forms.text(0, 100)

  def nonEmptySmallText = play.api.data.Forms.nonEmptyText(0, 100)

  def nonEmptyNotBlankSmallText = smallText.verifying("error.required", e => notBlank(e))

  def smallEmail = play.api.data.Forms.email.verifying("error.maxLength", e => isValidMaxLength(100)(e))

  def positiveInteger = number.verifying("error.positive.number", e => e >= 0)

  val phoneNumberErrorKey = "error.agent.phone"

  def validateMandatoryPhoneNumber = {
    s: String => s.matches("\\d+")
  }

  def validateOptionalPhoneNumber = {
    s: String => s.matches("\\d*")
  }

  def validateSaUtr = {
    s: String => s.matches("\\d{10}")
  }

  def notBlank(value: String) = !value.trim.isEmpty

  def isBlank(value: String) = !notBlank(value)

  def isValidMaxLength(maxLength: Int)(value: String): Boolean = value.length <= maxLength

  def isValidMinLength(minLength: Int)(value: String): Boolean = value.length >= minLength

  def isMainAddressLineLengthValid = isValidMaxLength(28)(_)

  def isOptionalAddressLineLengthValid = isValidMaxLength(18)(_)

  def isPostcodeLengthValid(value: String) = {
    val trimmedVal = value.replaceAll(" ", "")
    isValidMinLength(5)(trimmedVal) && isValidMaxLength(7)(trimmedVal)
  }
}


object characterValidator {
  //Valid Characters Alphanumeric (A-Z, a-z, 0-9), hyphen( - ), apostrophe ( ' ), comma ( , ), forward slash ( / ) ampersand ( & ) and space
  private val invalidCharacterRegex = """[^A-Za-z0-9,/'\-& ]""".r
  private val invalidPostCodeCharacterRegex = """[^A-Za-z0-9 ]""".r

  def containsValidPostCodeCharacters(value: String): Boolean = containsValidCharacters(value, invalidPostCodeCharacterRegex)

  def containsValidAddressCharacters(value: String): Boolean = containsValidCharacters(value, invalidCharacterRegex)

  private def containsValidCharacters(value: String, regex: Regex): Boolean = {
    regex.findFirstIn(value).isEmpty
  }
}

object DateFields {
  val day = "day"
  val month = "month"
  val year = "year"
}


object AddressFields {
  val addressLine1 = "addressLine1"
  val addressLine2 = "addressLine2"
  val addressLine3 = "addressLine3"
  val addressLine4 = "addressLine4"
  val postcode = "postcode"
}
